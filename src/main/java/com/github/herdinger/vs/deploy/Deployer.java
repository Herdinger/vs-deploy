package com.github.herdinger.vs.deploy;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.xml.namespace.QName;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Deployer {
    private List<String> pathComponents;
    private URI ownCloudUri;
    private Path warfile;
    private Sardine sardine;
    private Log log;


    Deployer(Log log, String deploymentPath, String ownCloudRoot, Sardine sardine, String warFile) {
        this.log = log;
        this.ownCloudUri = URI.create(ownCloudRoot + "/");
        this.sardine = sardine;
        this.warfile = Path.of(warFile);
        this.pathComponents = Arrays.stream(deploymentPath.split("/")).filter(component -> component.length() != 0).collect(Collectors.toList());
        if(this.pathComponents.lastIndexOf("container") != this.pathComponents.size() -1) {
            this.pathComponents.add("container");
        }

    }
    private String ownCloudLocation(Iterable<String> path) {
        String fullPath = String.join("/", path);
        return ownCloudUri.resolve(fullPath).toString();
    }

    private enum ResourceState {
        FOLDER, NON_FOLDER, NOT_FOUND
    }

    public void deploy() throws IOException, MojoExecutionException {
        String location = ownCloudLocation(pathComponents);
        //step 1 check if there is a folder or no resource yet.
        switch(resourceState(location)) {
            case FOLDER:
                log.debug("Found folder: " + location);
                deploy(pathComponents);
                break;
            case NON_FOLDER:
                throw new MojoExecutionException("Given deployment Path is not a folder");
            case NOT_FOUND:
                log.debug("Did not find folder folder: " + location);
                create(pathComponents);
                deploy(pathComponents);
                break;
        }

    }
    private void deploy(List<String> pathComponents) throws MojoExecutionException {
        List<String> warLocation = new ArrayList<>(pathComponents);
        warLocation.add(warfile.getFileName().toString());
        List<String> docekrLocation = new ArrayList<>(pathComponents);
        docekrLocation.add("Dockerfile");
        try(FileInputStream stream = new FileInputStream(warfile.toFile())) {
            log.info(String.format("uploading war file %s", ownCloudLocation(warLocation)));
            sardine.put(ownCloudLocation(warLocation), stream);
            log.info(String.format("uploading DOCKERFILE file %s", ownCloudLocation(docekrLocation)));
            sardine.put(ownCloudLocation(docekrLocation), dockerFile());
        } catch (FileNotFoundException e) {
            String error = "war file: " + warfile + " not found";
            throw new MojoExecutionException(error);
        } catch (IOException e) {
            String error = "error reading war file: " + warfile;
            throw new MojoExecutionException(error);
        }
    }

    private byte[] dockerFile() {
        String dockerString = "FROM jetty:9-jre11\n"
                            + "COPY " + warfile.getFileName().toString() + " /var/lib/jetty/webapps/ROOT.war";
        return dockerString.getBytes(Charset.forName("UTF-8"));
    }

    private void create(List<String> fullPath) throws IOException, MojoExecutionException {
        //after we have the first resource that does not exist we can just create without checking
        log.info("creating needed folders");
        Deque<String> base = new ArrayDeque<>();
        Deque<String> pathComponents = new ArrayDeque<>(fullPath);
        do {
            String currentFolder = pathComponents.peekFirst();
            List<String> currentLocationComponents = new ArrayList<>(base);
            currentLocationComponents.add(currentFolder);
            String  currentLocation = ownCloudLocation(currentLocationComponents);
            ResourceState resourceState = resourceState(currentLocation);
            if(resourceState == ResourceState.NON_FOLDER) {
                String error = String.format("%s already exists but is not a folder, aborting", currentLocation);
                throw new MojoExecutionException(error);
            }
            if(resourceState == ResourceState.NOT_FOUND) {
                log.debug(String.format("%s does not exists starting to create folders", currentLocation));
                break;
            }
            log.debug(String.format("%s already exists", currentLocation));
            base.addLast(pathComponents.removeFirst());
        } while(!pathComponents.isEmpty());
        //at this point we should have the path we need to create in pathComponents
        createNestedFolder(List.copyOf(base), List.copyOf(pathComponents));
    }

    private void createNestedFolder(List<String> base, List<String> folder) throws IOException {
        log.info(String.format("creating nested Folder base: %s, path: %s", String.join("/",base), String.join("/", folder)));
        for(int i=0; i< folder.size(); i++) {
            List<String> path = new ArrayList<>(base);
            path.addAll(folder.subList(0, i + 1));
            String location = ownCloudLocation(path);
            log.debug(String.format("creating %s", location));
            sardine.createDirectory(ownCloudLocation(path));
        }
    }


    private ResourceState resourceState(String uri) throws IOException {
        QName collectionType = new QName("DAV:", "collection");
        List<DavResource> a = null;
        try {
            a = sardine.list(uri,0);
        } catch (SardineException e) {
            if(e.getStatusCode() == 404) {
                return ResourceState.NOT_FOUND;
            }
            throw e;
        }
        List<QName> resourceTypes = a.get(0).getResourceTypes();
        if(resourceTypes.stream().anyMatch(qname -> qname.equals(collectionType))) {
            return ResourceState.FOLDER;
        } else {
            return ResourceState.NON_FOLDER;
        }
    }
}
