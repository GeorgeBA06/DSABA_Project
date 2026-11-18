package by.bsuir.supplier.security;
/** Invalid credentials/token. No details of the existence of accounts are exposed. */
public final class AuthenticationFailure extends RuntimeException {
    public AuthenticationFailure() {
        super("Неверные учётные данные или истёкшая сессия");
    }
}
