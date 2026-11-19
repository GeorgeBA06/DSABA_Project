import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeys {

    private static String toPem(String type, byte[] encoded) {
        String content = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded);

        return "-----BEGIN " + type + "-----\n" + content + "\n-----END " + type + "-----\n";
    }

    public static void main(String[] args) throws Exception {

        Path directory = Path.of("secrets");

        Files.createDirectories(directory);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

        generator.initialize(3072);

        KeyPair keyPair = generator.generateKeyPair();

        Files.writeString(
                directory.resolve("jwt-private.pem"),
                toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));

        Files.writeString(
                directory.resolve("jwt-public.pem"),
                toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));

        System.out.println("JWT keys generated successfully!");
        System.out.println("Private key: " + directory.resolve("jwt-private.pem").toAbsolutePath());
        System.out.println("Public key: " + directory.resolve("jwt-public.pem").toAbsolutePath());
    }
}
