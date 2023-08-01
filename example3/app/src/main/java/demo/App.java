package demo;

import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;

public class App{

    public static void main(String[] args) {

        VaultEndpoint vaultEndpoint = new VaultEndpoint();
        vaultEndpoint.setHost("127.0.0.1");
        vaultEndpoint.setPort(8200);
        vaultEndpoint.setScheme("http");

        VaultTemplate vaultTemplate = new VaultTemplate(
            vaultEndpoint,
            new TokenAuthentication("root"));

        Secrets secrets = new Secrets();
            secrets.username = "hello";
            secrets.password = "world";

        final String path = "demo-java/spring";

        vaultTemplate.write(path, secrets);

        VaultResponseSupport<Secrets> response = vaultTemplate.read(path, Secrets.class);

        System.out.println(response.getData().getUsername());
        System.out.println(response.getData().getPassword());

        // vaultTemplate.delete(path);
    }
}
