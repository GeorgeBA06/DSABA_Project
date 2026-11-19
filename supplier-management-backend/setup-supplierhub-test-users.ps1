# One-time setup for a LOCAL SupplierHub development database.
# Resets existing admin with BCrypt, revokes its sessions, then uses the
# application's REST API to create or update two role-specific test accounts.
# Do not run against a production database.
param(
    [string]$ApiUrl = "http://localhost:8081",
    [string]$PostgresContainer = "supplier-management-backend-postgres-1"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$ApiUrl = $ApiUrl.TrimEnd('/')
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)

function ConvertFrom-SecurePassword([System.Security.SecureString]$SecurePassword) {
    $ptr = [IntPtr]::Zero
    try {
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        if ($ptr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
        }
    }
}

function Read-LocalPassword([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    try {
        $plain = ConvertFrom-SecurePassword $secure
        if ($null -eq $plain -or $plain.Length -lt 12 -or $plain.Length -gt 128) {
            throw "Password must have 12-128 characters."
        }
        if ([Text.Encoding]::UTF8.GetByteCount($plain) -gt 72) {
            throw "BCrypt passwords must be at most 72 UTF-8 bytes."
        }
        return $plain
    }
    finally {
        if ($null -ne $secure) {
            $secure.Dispose()
        }
    }
}

function Invoke-SupplierApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Payload = $null,
        [hashtable]$Headers = @{}
    )
    $options = @{
        Method = $Method
        Uri = "$ApiUrl/api/v1$Path"
        Headers = $Headers
        ErrorAction = 'Stop'
    }
    if ($null -ne $Payload) {
        $json = ConvertTo-Json -InputObject $Payload -Depth 8 -Compress
        $options.ContentType = 'application/json; charset=utf-8'
        $options.Body = [Text.Encoding]::UTF8.GetBytes($json)
    }
    Invoke-RestMethod @options
}

function Find-TestUser([string]$Login, [hashtable]$Headers) {
    for ($index = 0; $index -lt 100; $index++) {
        $result = Invoke-SupplierApi -Method 'GET' -Path "/admin/users?page=$index&size=100" -Headers $Headers
        foreach ($item in @($result.items)) {
            if ($item.login -eq $Login) {
                return $item
            }
        }
        if ($index + 1 -ge [int]$result.totalPages) {
            return $null
        }
    }
    throw "The users listing has more than 100 pages; unable to locate $Login safely."
}

function Ensure-TestAccount {
    param(
        [string]$Login,
        [string]$DisplayName,
        [string]$Role,
        [hashtable]$Headers
    )

    $password = Read-LocalPassword "New password for $Login (hidden)"
    try {
        $user = Find-TestUser -Login $Login -Headers $Headers
        if ($null -eq $user) {
            $user = Invoke-SupplierApi -Method 'POST' -Path '/admin/users' -Headers $Headers -Payload @{
                login = $Login
                displayName = $DisplayName
                password = $password
                roles = @($Role)
            }
            Write-Host "Created: $Login ($Role)"
        }
        else {
            Invoke-SupplierApi -Method 'POST' -Path "/admin/users/$($user.id)/reset-password" -Headers $Headers -Payload @{
                newPassword = $password
            } | Out-Null
            if ($user.status -ne 'ACTIVE') {
                Invoke-SupplierApi -Method 'POST' -Path "/admin/users/$($user.id)/unblock" -Headers $Headers -Payload @{
                    reason = 'Local development test account setup'
                } | Out-Null
            }
            $existingRoles = @(Invoke-SupplierApi -Method 'GET' -Path "/admin/users/$($user.id)/roles" -Headers $Headers)
            if ($existingRoles -notcontains $Role) {
                Invoke-SupplierApi -Method 'POST' -Path "/admin/users/$($user.id)/roles" -Headers $Headers -Payload @{
                    role = $Role
                } | Out-Null
            }
            $otherRoles = @($existingRoles | Where-Object { $_ -ne $Role })
            if ($otherRoles.Count -gt 0) {
                Write-Warning "$Login also has other roles. Inspect them in the admin panel."
            }
            Write-Host "Updated: $Login ($Role)"
        }
        $tokens = Invoke-SupplierApi -Method 'POST' -Path '/auth/login' -Payload @{
            login = $Login
            password = $password
        }
        $profile = Invoke-SupplierApi -Method 'GET' -Path '/auth/me' -Headers @{
            Authorization = "Bearer $($tokens.accessToken)"
        }
        if (@($profile.roles) -notcontains $Role) {
            throw "Verification of $Role for $Login failed."
        }
        Write-Host "Login and role check: OK ($Login)" -ForegroundColor Green
    }
    finally {
        Remove-Variable password -ErrorAction SilentlyContinue
    }
}

$health = Invoke-RestMethod -Method 'GET' -Uri "$ApiUrl/actuator/health" -ErrorAction Stop
if ($health.status -ne 'UP') {
    throw 'Backend is not healthy.'
}

$repo = Join-Path $env:USERPROFILE '.m2\repository\org\springframework\security\spring-security-crypto'
if (-not (Test-Path $repo)) {
    throw "spring-security-crypto.jar not found in $repo. Run Maven once before this script."
}
$jar = Get-ChildItem $repo -Recurse -File -Filter 'spring-security-crypto-*.jar' |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $jar) {
    throw "spring-security-crypto.jar not found in $repo"
}
# Temporary source helper; no additional project files are required.
$javaDir = Join-Path ([IO.Path]::GetTempPath()) ('supplierhub-bcrypt-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($javaDir) | Out-Null
$javaFile = Join-Path $javaDir 'LocalBCrypt.java'
$javaSource = @'
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Local-only utility: reads a password through stdin, prints only a BCrypt hash. */
public class LocalBCrypt {
    public static void main(String[] args) throws Exception {
        String password = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
        ).readLine();
        if (password == null || password.length() < 12 || password.length() > 128
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            System.err.println("Password must be 12-128 characters and at most 72 UTF-8 bytes.");
            System.exit(1);
        }
        System.out.print(BCrypt.hashpw(password, BCrypt.gensalt(12)));
    }
}
'@
[IO.File]::WriteAllText($javaFile, $javaSource, ([Text.UTF8Encoding]::new($false)))

Write-Host "Backend: $ApiUrl (UP)"
Write-Host 'Resetting password for the existing admin in the LOCAL database.'
$adminPassword = Read-LocalPassword 'New password for admin (hidden)'
try {
    $hash = ($adminPassword | & java -cp $jar.FullName $javaFile)
    if ($LASTEXITCODE -ne 0) {
        throw 'Java BCrypt generation failed.'
    }
    $hash = ([string]$hash).Trim()
    if ($hash -notmatch '^\$2[aby]\$12\$[./A-Za-z0-9]{53}$') {
        throw 'Generated password hash has an unexpected format.'
    }

    # The hash has a strictly validated alphabet and cannot contain SQL quotes.
    # Only the existing admin record and its sessions are modified.
    $sql = @'
BEGIN;
DO $body$
BEGIN
    IF (SELECT COUNT(*) FROM users WHERE login = 'admin') <> 1 THEN
        RAISE EXCEPTION 'Expected exactly one existing admin user';
    END IF;
END
$body$;
UPDATE users
SET password_hash = '__HASH__',
    failed_login_count = 0,
    locked_until = NULL,
    updated_at = NOW(),
    lock_version = lock_version + 1
WHERE login = 'admin';
UPDATE auth_sessions
SET revoked_at = NOW(),
    lock_version = lock_version + 1
WHERE user_id = (SELECT id FROM users WHERE login = 'admin')
  AND revoked_at IS NULL;
COMMIT;
'@
    $sql = $sql.Replace('__HASH__', $hash)
    $sql | & docker exec -i $PostgresContainer psql -X -v ON_ERROR_STOP=1 -U supplier -d supplier_management -f -
    if ($LASTEXITCODE -ne 0) {
        throw 'PostgreSQL password reset failed. Please inspect the error above.'
    }
    Write-Host 'Admin password updated, old sessions revoked.' -ForegroundColor Green

    $adminTokens = Invoke-SupplierApi -Method 'POST' -Path '/auth/login' -Payload @{
        login = 'admin'
        password = $adminPassword
    }
    $authHeader = @{ Authorization = "Bearer $($adminTokens.accessToken)" }
    $me = Invoke-SupplierApi -Method 'GET' -Path '/auth/me' -Headers $authHeader
    if (@($me.roles) -notcontains 'ADMINISTRATOR') {
        throw 'Admin logged in but lacks the ADMINISTRATOR role.'
    }
    Write-Host 'Admin login and role check: OK' -ForegroundColor Green
}
finally {
    Remove-Variable adminPassword, hash, sql -ErrorAction SilentlyContinue
    Remove-Item $javaDir -Recurse -Force -ErrorAction SilentlyContinue
}

Ensure-TestAccount -Login 'test.manager' -DisplayName 'Test Procurement Manager' -Role 'PROCUREMENT_MANAGER' -Headers $authHeader
Ensure-TestAccount -Login 'test.specialist' -DisplayName 'Test Procurement Specialist' -Role 'PROCUREMENT_SPECIALIST' -Headers $authHeader

Write-Host ''
Write-Host 'All three accounts are ready for the SupplierHub login screen:' -ForegroundColor Green
Write-Host '  admin            = ADMINISTRATOR'
Write-Host '  test.manager     = PROCUREMENT_MANAGER'
Write-Host '  test.specialist  = PROCUREMENT_SPECIALIST'
Write-Host 'Use the three passwords you just entered. Do not share them.'
