# Deploys a built WAR or Fat-JAR to the VS Cloud
## Requirements
The project builds a WAR or Fat-JAR (Jar with dependencies included) file.
### Example: Spark
To make your maven Spark project produce a WAR, just add
```xml
    <packaging>war</packaging>
```
Inside of 
```xml
<project></project>
```
of your pom.xml

If you want to directly run the WAR you can also add
```xml
    <plugin>
        <groupId>org.eclipse.jetty</groupId>
        <artifactId>jetty-maven-plugin</artifactId>
        <version>9.3.9</version>
    </plugin>
```
inside of
```xml
<build>
    <plugins>
    </plugins>
</build>
```
of your pom.xml

This enables the jetty:run task which starts your app locally.
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
            <groupId>com.github.herdinger</groupId>
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
## Configuration Options
The serverid used for user and password in your settings.xml
```xml
<serverId></serverId>
```
The environment variables that get set for your container
```xml
<env><EXAMPLE_KEY1>exampleValue</EXAMPLE_KEY1><KEY2>Value2</KEY2></env>
```
The ports you want to expose
```xml
<ports><param>8080</param></ports>
```
The path in the owncloud you want to copy your files too
```xml
<deploymentPath></deploymentPath>
```
The path to owncloud
```xml
<ownCloudRoot></ownCloudRoot>
```
The path to the vs cloud
```xml
<vsCloudURL></vsCloudURL>
```
