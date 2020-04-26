package com.github.herdinger.vs.deploy;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo( name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class Plugin extends AbstractMojo
{
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;
    @Parameter( required = true )
    private String serverId;
    @Parameter( required = true )
    private String deploymentPath;
    @Parameter( required = true )
    private String ownCloudRoot;
    @Parameter( required = true )
    private String vsCloudURL;
    @Parameter
    private List<String> ports;
    @Parameter
    private Map<String,String> env;
    @Parameter( defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}", required = true )
    private String artifact;
    @Parameter( defaultValue = "${project.packaging}", required = true )
    private String packaging;
    private boolean isWar;
    private URI ownCloudUri;

    public void execute() throws MojoExecutionException
    {
        isWar = packaging.equals("war");
        try {
            ownCloudUri = new URI(ownCloudRoot);
            if(!ownCloudUri.getScheme().equals("https")) {
                throw new MojoExecutionException("insecure onwCloudURI please use https:// instead of " + ownCloudUri.getScheme());
            }
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("invalid ownCloudRoot");
        }
        if (ports == null) {
            ports = Collections.emptyList();
        }
        if(env == null) {
            env = Collections.emptyMap();
        }
        Server server = settings.getServer(serverId);
        server.getUsername();
        server.getPassword();
        String user = server.getUsername();
        String pass = server.getPassword();
        Sardine uploader = SardineFactory.begin(user,pass);
        Deployer deployer = new Deployer(getLog(), deploymentPath, ownCloudUri.toString(), uploader, artifact, isWar, ports, env);
        getLog().info("deploying");
        try {
            deployer.deploy();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        getLog().info("building");
        Builder.build(getLog(),user,pass,deploymentPath, vsCloudURL);
    }
}
