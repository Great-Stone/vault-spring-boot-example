# Spring Boot Configuration(Secret) Management

> Example Source : Attach to email
>
> Example Environment
>
> - Vault 1.13.1
> - OpenJDK 11.0.18
> - Gradle 8.0.2



This section describes two ways to integrate Vault in a Spring-boot environment. The main approach is to use Java libraries.

- Using `Spring Cloud Vault` to import configuration values stored in the vault into the configuration of Spring Boot/Cloud.
- Using Spring Vault Core to integrate with the vault codewise throughout the Spring environment.

In addition to this, you can make commands to the vault directly via the `API`(<https://developer.hashicorp.com/vault/api-docs>).



## [Example 1. Spring Boot Application]

The first example describes the process of setting up a vault server and using the vault to read configuration information from Spring Boot. To configure the vault for your app, run the vault as follows

```bash
$ vault server -dev -dev-root-token-id=root -log-level=trace

...
You may need to set the following environment variables:

    $ export VAULT_ADDR='http://127.0.0.1:8200'

The unseal key and root token are displayed below in case you want to
seal/unseal the Vault or re-authenticate.

Unseal Key: UTZ7HoZCu8dtWa/eSMKcwq1klhC/qFoDxHXmhRn4qnE=
Root Token: root
```

If using the enterprise binary, run it with the licence pass as an environment variable `VAULT_LICENSE_PATH`.

```bash
$ export VAULT_LICENSE_PATH=<path>/vault.hclic
$ vault server -dev -dev-root-token-id=root -log-level=trace
...
```

The enterprise binary checks for paths prefixed with `+ent` at the following link: (<https://releases.hashicorp.com/vault/>)



The `root` token is assumed to be the privilege of the configuration management administrator.

```bash
$ export VAULT_ADDR='http://127.0.0.1:8200'
$ vault login
Token (will be hidden): root

Success! You are now authenticated. The token information displayed below
is already stored in the token helper. You do NOT need to run "vault login"
again. Future Vault requests will automatically use this token.

Key                  Value
---                  -----
token                root
token_accessor       w5LvrjTvDDcfjPHrnOj6ib7E
token_duration       ∞
token_renewable      false
token_policies       ["root"]
identity_policies    []
policies             ["root"]
```

Enable the KV for use in the Spring Boot app.

```bash
$ vault secrets enable -path=demo-app -version=2 kv

Success! Enabled the kv secrets engine at: demo-app/
```



The example assumes you are managing MySQL information in configuration management. The associated Spring Boot app is created via [spring initializr](https://start.spring.io).

![image-20230406150032627](https://raw.githubusercontent.com/Great-Stone/images/master/uPic/image-20230406150032627.png)

The list of dependencies for the test looks like this

| Dependencies        | Description                                                         |
| ------------------- | ------------------------------------------------------------ |
| Spring Web          | Using Spring MVC to build web applications including RESTful |
| MySQL Driver        | Driver for using MySQL (omit if you don't have MySQL)        |
| Spring Data JPA     | Modules to make JPA easy to use (omit if you don't have MySQL) |
| Vault Configuration | Provide client-side support for configuring externalised vaults on distributed systems |
| Lombok              | A library in Java that automates the writing of mechanical code based on annotations. |



For MySQL, create a user like this

```sql
CREATE DATABASE java_dev_db;
CREATE USER 'dev-user'@'%' IDENTIFIED BY 'dev-password';
GRANT ALL PRIVILEGES ON java_dev_db.* TO 'dev-user'@'%';
```



Add the configuration you want your app to use to `demo-app/java_and_vault/dev` in the vault. The combination of endpoint information is `<kv_endpoint>/<app_name>/<profile>`. Add configuration information using the CLI as follows.

```bash
$ vault kv put demo-app/java_and_vault/dev \
		app.config.auth.token=MY-AUTH-TOKEN-DEV-0000 \
		app.config.auth.username=dev-user \
		spring.datasource.database=java_dev_db \
		spring.datasource.password=dev-password \
		spring.datasource.username=dev-user
```

In the UI, the result looks like this The above CLI operations are also available in the UI.

- In an internet browser, navigate to <http://127.0.0.1:8200>, the address of your vault
- Enter the `root` token obtained during the previous run in development mode in the `Token` method of authentication.
- Verify the value in the `demo-app` secret engine

![image-20230407093720360](https://raw.githubusercontent.com/Great-Stone/images/master/uPic/image-20230407093720360.png)



To configure the vault integration with your app, add the following In the example, change the configuration to `application.yml` instead of the default `application.properties`.

```yaml
spring:
  application:
    name: java_and_vault
  cloud.vault:
      host: 127.0.0.1
      port: 8200
      scheme: http
      config:
        lifecycle:
          enabled: false
      authentication: TOKEN
      token: root
      kv:
        enabled: true
        backend: demo-app
        profile-separator: '/'
      generic:
        enabled: false
  config:
    import: vault://
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/${spring.datasource.database}
```

- Vault-specific settings are added to `spring.cloud.vault`.
  - `host` : Set the hostname or IP of the Vault server.
  - `port` : Set the port of the Vault server.
  - `scheme` : Set the protocol to use for communication with the Vault server.
  - For `config.lifecycle.enabled`, set whether lifecycle management behaviour is enabled for dynamic secrets. We're using a static configuration, so set it to `false`.
- Enter `spring.cloud.vault.authentication` as `TOKEN` for administrator testing.
- For `spring.cloud.vault.token`, enter `root`, the administrator authentication.
- The `spring.cloud.vault.kv` is a hierarchy for the declaration of enabled KVs.
  - `enalbed` : Sets whether to enable to a boolean value.
  - `backend` : Type the path name of the endpoint where KV is enabled. The default value is `secret`.
- The `spring.cloud.vault.generic` is a hierarchy for KV declarations of type v1.
  - `enalbed` : Set to a boolean value whether it is enabled. Set to `false` as it is not used.
- Mount the vault as a PropertySource by specifying `vault://` in `spring.config.import`.
- Set the definitions for MySQL integration in `spring.datasource`.
  - `url` : Specify the DB Connection Url.
  - `database` : Define the name of the DB. In this case, we're getting that value from the vault.
  - `username` : Define the DB account username. This is omitted here because we get that value from the vault.
  - `password` : Defines the DB account user password. This is omitted here because we get that value from the vault.



Add the following Java files to the default package path (e.g. `src/main/java/com/example/demo`).

| AppConfiguration.java

```java
package com.example.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Configuration
@ConfigurationProperties("app.config.auth")
public class AppConfiguration {
    private String username;
    private String token;
}
```

- Inject the contents of the vault mounted with `app.config.auth` defined in `@ConfigurationProperties`.
- The `AppConfiguration` class is assigned the values of the variables `username` and `token`, which are defined internally from the Vault according to the annotation definition.



| AppService.java

```java
package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {
    private final AppConfiguration appConfiguration;
    @PostConstruct
    public void readConfigs() {
        log.info("Reading configuration {} - {}", appConfiguration.getToken(), appConfiguration.getUsername());
    }
}
```

- Check the values of variables assigned from the vault in the log output to the `readConfigs()'` method.



| DemoApplication.java

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class DemoApplication {

	@Value("${spring.datasource.username}")
	private String ds_name;

	@Value("${spring.datasource.password}")
	private String ds_pw;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@PostConstruct
	public void readDBconfigs() {
			log.info("Reading datasource config {} - {}", ds_name, ds_pw);
	}
}
```

- The `@Value` gets the value injected into the configuration information that should be defined in the `application.yml`.
- Use the `readDBconfigs()` method to check the log output for the configuration values assigned by the Vault.



Run the app to see if it imports the configuration.

```bash
$ gradle bootRun --args='--spring.profiles.active=dev'

> Task :bootRun

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.0.5)

# Indicate that a dev profile is used
2023-04-06T17:15:58.395+09:00  INFO 48275 --- [           main] 
com.example.demo.DemoApplication         : The following 1 profile is active: "dev"

# Verify that the information defined in spring.datasource in the app configuration is fetched from the vault and executed to create the Connection Pool and output the imported account information.
2023-04-06T17:16:00.359+09:00  INFO 48275 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2023-04-06T17:16:00.614+09:00  INFO 48275 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@57416e49
2023-04-06T17:16:00.616+09:00  INFO 48275 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
...
2023-04-07T08:57:39.888+09:00  INFO 52598 --- [           main] com.example.demo.DemoApplication         : Reading datasource config dev-user - dev-password

# Get the app configuration app.config.auth entry from the vault and see the output
2023-04-06T17:16:01.363+09:00  INFO 48275 --- [           main] com.example.demo.AppService              : Reading configuration MY-AUTH-TOKEN-DEV-0000 - dev-user
```



## [Example 2. Spring Boot Application + RBAC]

In the first example, you can see all configuration values using the Vault's `root` user, but our app, the people who deploy it, and the pipeline should only be able to see information about specific configurations. The second example shows the configuration and policy definitions for the `prd` profile.



For MySQL, create a user like this

```sql
CREATE DATABASE java_prd_db;
CREATE USER 'prd-user'@'%' IDENTIFIED BY 'prd-password';
GRANT ALL PRIVILEGES ON java_prd_db.* TO 'prd-user'@'%';
```

Add configuration information for `prd` to the vault.

```bash
$ vault kv put demo-app/java_and_vault/prd \
		app.config.auth.token=MY-AUTH-TOKEN-prd-1111 \
		app.config.auth.username=prd-user \
		spring.datasource.database=java_prd_db \
		spring.datasource.password=prd-password \
		spring.datasource.username=prd-user
```

![image-20230407094227005](https://raw.githubusercontent.com/Great-Stone/images/master/uPic/image-20230407094227005.png)

The Policy `java-and-vault-prd-admin.hcl` file contents and application for the configuration manager are as follows.

```bash
$ cat java-and-vault-prd-admin.hcl

path "demo-app/data/java_and_vault/prd" {
  capabilities = ["create", "update", "read"]
}

$ vault policy write java-and-vault-prd-admin java-and-vault-prd-admin.hcl

Success! Uploaded policy: java-and-vault-prd-admin
```



The configuration-read-only policy file `java-and-vault-prd-read.hcl` has the following contents and application

```bash
$ cat java-and-vault-prd-read.hcl

path "demo-app/data/java_and_vault/prd" {
  capabilities = ["read"]
}

$ vault policy write java-and-vault-prd-read java-and-vault-prd-read.hcl

Success! Uploaded policy: java-and-vault-prd-read
```



The contents of the `java-and-vault-prd-approle.hcl` file, which is the policy for issuing accounts for the app, are as follows.

```bash
$ cat java-and-vault-prd-approle.hcl

path "auth/approle/role/java-vault-prd/role-id" {
  capabilities = ["read"]
}

path "auth/approle/role/java-vault-prd/secret-id" {
  capabilities = ["create", "update"]
}

$ vault policy write java-and-vault-prd-approle java-and-vault-prd-approle.hcl

Success! Uploaded policy: java-and-vault-prd-approle
```



Give the administrator `java-and-vault-prd-admin`, `java-and-vault-prd-approle` to manage the configuration and issue accounts for the app.

```bash
# Enable userpass Auth Method if not already enabled
$ vault auth enable userpass

Success! Enabled userpass auth method at: userpass/

$ vault write auth/userpass/users/app-prd-admin password=password policies=java-and-vault-prd-admin,java-and-vault-prd-approle

Success! Data written to: auth/userpass/users/app-prd-admin
```



Add `java-and-vault-prd-read` to the AppRole authentication for your app.

```bash
# Enable approle Auth Method if not already enabled
$ vault auth enable approle

Success! Enabled approle auth method at: approle/

$ vault write auth/approle/role/java-vault-prd \
    secret_id_ttl=10m \
    token_period=24h \
    policies="java-and-vault-prd-read"
    
Success! Data written to: auth/approle/role/java-vault-prd
```



Log in with the admin account you created and verify that you can change the configuration of `demo-app/java_and_vault/prd` and issue a `secret-id` for the AppRole account. (Separate terminal)

```bash
$ export VAULT_ADDR=http://127.0.0.1:8200
$ vault login -method userpass username=app-prd-admin password=password

Success! You are now authenticated. The token information displayed below
is already stored in the token helper. You do NOT need to run "vault login"
again. Future Vault requests will automatically use this token.

Key                    Value
---                    -----
token                  hvs.CAESIAE31Vrf91UbPhV5O0eh8KM0Tky_7MGk5ThyRu4tJbhUGh4KHGh2cy50ZDdZZ09BdDRnRmpqdkVRcUJYOWR5YUI
token_accessor         9XuvRw1jKWt99iwlZ146652v
token_duration         768h
token_renewable        true
token_policies         ["default" "java-and-vault-prd-admin" "java-and-vault-prd-approle"]
identity_policies      []
policies               ["default" "java-and-vault-prd-admin" "java-and-vault-prd-approle"]
token_meta_username    app-prd-admin

$ vault kv put demo-app/java_and_vault/prd \
    app.config.auth.token=MY-AUTH-TOKEN-prd-1111 \
    app.config.auth.username=prd-user \
    spring.datasource.database=java_prd_db \
    spring.datasource.password=prd-password \
    spring.datasource.username=prd-user
    
========== Secret Path ==========
demo-app/data/java_and_vault/prd

======= Metadata =======
Key                Value
---                -----
created_time       2023-04-07T01:54:45.464698Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            2

$ vault read auth/approle/role/java-vault-prd/role-id

Key        Value
---        -----
role_id    53b96749-1234-fec1-05b8-760c29991d89

$ vault write -f auth/approle/role/java-vault-prd/secret-id

Key                   Value
---                   -----
secret_id             69b144ae-543a-81e3-9afa-8b290d8efd75
secret_id_accessor    d9338290-f1ff-ca09-fbaf-742071afeaa6
secret_id_num_uses    0
secret_id_ttl         10m
```



Log in with the AppRole account you'll be using in your app and verify that the configuration changes in `demo-app/java_and_vault/prd` can be read but not updated. (Separate terminal)

```bash
$ export VAULT_ADDR=http://127.0.0.1:8200
$ vault write auth/approle/login \
    role_id=53b96749-1234-fec1-05b8-760c29991d89 \
    secret_id=aebbc4ac-79e4-c529-8751-c52f2f31a3d7

Key                     Value
---                     -----
token                   hvs.CAESIC7bpDI_cDGLCpKl6rZ
token_accessor          guDRqHNpnJtpmFXqkqsahc2e
token_duration          24h
token_renewable         true
token_policies          ["default" "java-and-vault-prd-read"]
identity_policies       []
policies                ["default" "java-and-vault-prd-read"]
token_meta_role_name    java-vault-prd

# The account for the app has read permission in its granted permissions, so it can see the information
$ VAULT_TOKEN=hvs.CAESIC7bpDI_cDGLCpKl6rZ vault kv get demo-app/java_and_vault/prd

========== Secret Path ==========
demo-app/data/java_and_vault/prd

======= Metadata =======
Key                Value
---                -----
created_time       2023-04-07T01:54:45.464698Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            2

=============== Data ===============
Key                           Value
---                           -----
app.config.auth.token         MY-AUTH-TOKEN-prd-1111
app.config.auth.username      prd-user
spring.datasource.database    java_prd_db
spring.datasource.password    prd-password
spring.datasource.username    prd-user

# Accounts for apps don't have write permissions in their granted permissions, so deny permissions on related requests
$ VAULT_TOKEN=hvs.CAESIC7bpDI_cDGLCpKl6rZ vault kv put demo-app/java_and_vault/prd \
    app.config.auth.token=MY-AUTH-TOKEN-prd-2222

Error writing data to demo-app/data/java_and_vault/prd: Error making API request.

URL: PUT http://127.0.0.1:8200/v1/demo-app/data/java_and_vault/prd
Code: 403. Errors:

* 1 error occurred:
	* permission denied
```



Modify `application.yml` to configure vault integration with your app and policies.

```yaml
spring:
  application:
    name: java_and_vault
  cloud.vault:
      host: 127.0.0.1
      port: 8200
      scheme: http
      config:
        lifecycle:
          enabled: false
      # authentication: TOKEN
      # token: root
      authentication: APPROLE
      app-role:
        role-id: 53b96749-1234-fec1-05b8-760c29991d89
        secret-id: aebbc4ac-79e4-c529-8751-c52f2f31a3d7
        role: db-kv-reader
        app-role-path: approle
      kv:
        enabled: true
        backend: demo-app
        profile-separator: '/'
      generic:
        enabled: false
  config:
    import: vault://
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/${spring.datasource.database}
```

- spring.cloud.vault.authentication` sets `APPROLE`, the method you created with authentication for your app.
- The `spring.cloud.vault.authentication.app-role` is the layer for declaring the `APPROLE` authentication.
  - `role-id` : Set the issued `role-id`.
  - `secret-id` : Set the issued `secret-id`. The `secret-id` has a timeout of `10m`, so it will be replaced every deployment to protect your account.
  - `role` : Set the name of the Approle that contains the `role-id`.
  - `app-role-path` : Enter the endpoint path name of the enabled approach.



Run the app to see if it imports the configuration.  Specify the `prd` profile.

```bash
$ gradle bootRun --args='--spring.profiles.active=prd'                                                                      

> Task :bootRun

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.0.5)

# Indicate that "prd" profile is used
2023-04-07T14:05:03.395+09:00  INFO 67782 --- [           main] com.example.demo.DemoApplication         : The following 1 profile is active: "prd"

# Verify that the information defined in spring.datasource in the app configuration outputs the account information taken from the vault
2023-04-07T14:05:05.099+09:00  INFO 67782 --- [           main] com.example.demo.DemoApplication         : Reading datasource config prd-user - prd-password

# Get the app configuration "app.config.auth" entry from the vault and see the output
2023-04-07T14:05:05.103+09:00  INFO 67782 --- [           main] com.example.demo.AppService              : Reading configuration MY-AUTH-TOKEN-prd-1111 - prd-user
```



If you specify an unprivileged `dev` profile, the configuration values will not be fetched, resulting in an error when the app runs.



## [Example 3. Spring Vault]

> Building Java Applications Sample : <https://docs.gradle.org/current/samples/sample_building_java_applications.html>
> Spring Vault : <https://docs.spring.io/spring-vault/docs/current/reference/html/#reference-documentation>

A `Spring Vault` can use the abstracted environment of a vault.

Log in as `root` to the vault environment used in examples 1 and 2 to create the KV environment used in example 3.

```bash
$ export VAULT_ADDR='http://127.0.0.1:8200'
$ vault login
Token (will be hidden): root

Success! You are now authenticated. The token information displayed below
is already stored in the token helper. You do NOT need to run "vault login"
again. Future Vault requests will automatically use this token.

Key                  Value
---                  -----
token                root
token_accessor       w5LvrjTvDDcfjPHrnOj6ib7E
token_duration       ∞
token_renewable      false
token_policies       ["root"]
identity_policies    []
policies             ["root"]

# Create a Version 1 KV.
$ vault secrets enable -path=demo-java -version=1 kv

Success! Enabled the kv secrets engine at: demo-java/
```



Initialise your Java app environment with Gradle.

```bash
$ gradle init
Starting a Gradle Daemon, 1 incompatible Daemon could not be reused, use --status for details

Select type of project to generate:
  1: basic
  2: application
  3: library
  4: Gradle plugin
Enter selection (default: basic) [1..4] 2

Select implementation language:
  1: C++
  2: Groovy
  3: Java
  4: Kotlin
  5: Scala
  6: Swift
Enter selection (default: Java) [1..6] 3

Split functionality across multiple subprojects?:
  1: no - only one application project
  2: yes - application and library projects
Enter selection (default: no - only one application project) [1..2] 1

Select build script DSL:
  1: Groovy
  2: Kotlin
Enter selection (default: Groovy) [1..2] 1

Generate build using new APIs and behavior (some features may change in the next minor release)? (default: no) [yes, no] no

Select test framework:
  1: JUnit 4
  2: TestNG
  3: Spock
  4: JUnit Jupiter
Enter selection (default: JUnit Jupiter) [1..4] 4

Project name (default: example3): demo
Source package (default: demo): demo

> Task :init
Get more help with your project: https://docs.gradle.org/8.0.2/samples/sample_building_java_applications.html

BUILD SUCCESSFUL in 31s
2 actionable tasks: 2 executed

# Check execution after initialisation
$ gradle run

> Task :app:run
Hello World!

BUILD SUCCESSFUL in 5s
2 actionable tasks: 2 executed
```



Add a dependency for `spring-vault-core` to the `app/src/build.gradle` file.

```gradle
plugins {
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.1'

    implementation 'com.google.guava:guava:31.1-jre'
    implementation 'org.springframework.vault:spring-vault-core:3.0.2'
}

application {
    mainClass = 'demo.App'
}

tasks.named('test') {
    useJUnitPlatform()
}
```



Define a `Secrets` class for the Vault KV in `app/src/main/java/demo/Secrets.java`.

```java
package demo;

public class Secrets {

    String username;
    String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
```



In `app/src/main/java/demo/App.java`, write the following code to write/read/delete configuration to the vault.

```java
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
```

The Spring Cloud Vault used in Examples 1 and 2 handles vault information and logins, but the difference is that Spring Vault also allows you to create and manage configurations for the vault.

We run `vaultTemplate.delete(path);` commented out, so the configuration that is added will remain in the vault. The result is the following

```bash
$ gradle run

> Task :app:run
hello
world

BUILD SUCCESSFUL in 1s
3 actionable tasks: 2 executed, 1 up-to-date
```

![image-20230413103923798](https://raw.githubusercontent.com/Great-Stone/images/master/uPic/image-20230413103923798.png)

