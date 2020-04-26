package com.github.herdinger.vs.deploy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Base64;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Builder {
    static class AddAuthHeadersRequestFilter implements ClientRequestFilter {

        private final String username;
        private final String password;

        AddAuthHeadersRequestFilter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void filter(ClientRequestContext requestContext) {
            String token = username + ":" + password;
            String base64Token = Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8));
            requestContext.getHeaders().add("Authorization", "Basic " + base64Token);
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Buildable {
        public String id;
        public String path;
    }
    static class Buildables {
        public List<Buildable> buildables;
    }

    static class OwncloudPath {
        public String refresh;
        String getPath() {
            return refresh.replace("owncloud://", "");
        }
    }

    static class Import {
        @JsonProperty("import")
        public String path;
        Import(String path) {
            this.path = path;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Consturciton {
        public String id;
    }
    interface VSPD
    {
        @GET
        @Path("buildables")
        @Produces("application/json")
        Buildables buildables();
        @POST
        @Path("buildables")
        @Produces("application/json")
        @Consumes("application/json")
        Buildables create(Import i);
        @POST
        @Path("buildables/{id}/refresh")
        @Produces("application/json")
        String refresh(@PathParam("id") String id);
        @GET
        @Path("buildables/{id}/refresh")
        @Produces("application/json")
        Response owncloudPath(@PathParam("id") String id);
        @POST
        @Path("buildables/{id}")
        @Produces("application/json")
        Consturciton build(@PathParam("id") String id);
        @GET
        @Path("constructions/{id}/stdout.stream")
        @Produces("application/json")
        InputStream construction(@PathParam("id") String id);
    }
    static void build(Log log, String user, String pass, String deploymentPath, String cloudUrl) throws MojoExecutionException {
        List<String> pathComponents = Arrays.stream(deploymentPath.split("/")).filter(component -> component.length() != 0).collect(Collectors.toList());
        if(pathComponents.lastIndexOf("container") == pathComponents.size() - 1) {
            pathComponents.remove(pathComponents.size()-1);
        }
        deploymentPath = "/" + String.join("/", pathComponents);
        log.debug(deploymentPath);
        ResteasyClient client = (ResteasyClient) ClientBuilder.newClient();
        client.register(new AddAuthHeadersRequestFilter(user, pass));
        ResteasyWebTarget target = client.target(cloudUrl);
        VSPD vsClient = target.proxy(VSPD.class);
        List<Buildable> buildables = vsClient.buildables().buildables;
        Buildable deployedBuildable = null;
        for (Buildable b : buildables) {
            Response r = vsClient.owncloudPath(b.id);
            if (r.getStatus() == 200) {
                if (deploymentPath.equals(r.readEntity(OwncloudPath.class).getPath())) {
                    deployedBuildable = b;
                    break;
                }
            }
        }
        if (deployedBuildable == null) {
            log.info("Did not find deployed buildable");
            log.info("Creating...");
            String containerName = Paths.get(deploymentPath).getFileName().toString();
            log.debug(containerName);
            log.debug(deploymentPath);
            buildables = vsClient.create(new Import("owncloud://" + deploymentPath)).buildables;
            int tries = 1;
            while (buildables.stream().noneMatch(buildable -> buildable.path.endsWith(containerName))) {
                buildables = vsClient.buildables().buildables;
                tries++;
                if(tries == 4) {
                    throw new MojoExecutionException("was unable to create deployable");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new MojoExecutionException("was unable to create deployable");
                }
            }
            deployedBuildable = buildables.stream().filter(buildable -> buildable.path.endsWith(containerName)).findFirst().get();
        }
        System.out.println("Refreshing...");
        String r = vsClient.refresh(deployedBuildable.id);
        Consturciton c = vsClient.build(deployedBuildable.id);
        InputStream is = vsClient.construction(c.id);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while (true) {
            try {
                if (((line = reader.readLine()) == null)) break;
            } catch (IOException e) {
                throw new MojoExecutionException(e, "fail", "fail");
            }
            log.info(line);
        }
    }
}
