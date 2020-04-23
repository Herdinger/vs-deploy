# Deploys a built WAR to the VS Cloud
## Requirements
The project builds a WAR file.
## Usage
Edit your settings.xml (usually located in $HOME/.m2) to add your Login credentials.
Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
    <servers>
        <server>
            <id>owncloud</id>
            <username>user</username>
            <password>password</password>
        </server>
    </servers>
</settings>
```
In your project pom.xml include this Plugin and remove the default deployment plugin

Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<build>
    <plugins>
        <plugin>
            <groupId>com.github.herdinger.vs.deploy</groupId>
            <artifactId>vs-deploy</artifactId>
            <version>1.0</version>
            <executions>
                <execution>
                    <id>Deploy and build</id>
                    <configuration>
                        <deploymentPath>/folder/container_name</deploymentPath>
                        <ownCloudRoot>https://exampleRoot.com/remote.php/webdav/</ownCloudRoot>
                        <vsCloudURL>https://vs.xxx.yy</vsCloudURL>
                        <serverId>owncloud</serverId>
                    </configuration>
                    <goals>
                        <goal>deploy</goal>
                    </goals>
                    <phase>deploy</phase>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <artifactId>maven-deploy-plugin</artifactId>
            <version>2.7</version>
            <configuration>
                <skip>true</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```
