package org.safehaus.penrose.federation.module;

import org.safehaus.penrose.module.Module;
import org.safehaus.penrose.federation.*;
import org.safehaus.penrose.partition.PartitionContext;
import org.safehaus.penrose.partition.PartitionManager;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.filters.ExpandProperties;

import java.util.Collection;
import java.util.ArrayList;
import java.io.File;

/**
 * @author Endi Sukma Dewata
 */
public class FederationModule extends Module implements FederationMBean {

    public final static String CONFIG             = "config";
    public final static String REPOSITORY_TYPES   = "repositoryTypes";
    public final static String CONFLICT_DETECTION = "conflictDetections";

    FederationReader reader = new FederationReader();
    FederationWriter writer = new FederationWriter();

    String config = "federation.xml";
    Collection<String> repositoryTypes = new ArrayList<String>();
    Collection<String> conflictDetections = new ArrayList<String>();

    FederationConfig federationConfig = new FederationConfig();
    File partitionsDir;

    public void init() throws Exception {

        String s = getParameter(CONFIG);
        if (s != null) {
            log.debug("Config: "+s);
            config = s;
        }

        s = getParameter(REPOSITORY_TYPES);
        if (s != null) {
            log.debug("Repository types:");
            for (String type : s.split(",")) {
                log.debug(" - "+type);
                repositoryTypes.add(type);
            }
        }

        s = getParameter(CONFLICT_DETECTION);
        if (s != null) {
            log.debug("Conflict detections:");
            for (String type : s.split(",")) {
                log.debug(" - "+type);
                conflictDetections.add(type);
            }
        }

        PartitionContext partitionContext = partition.getPartitionContext();
        File path = partitionContext.getPath();

        File dir = path == null ?
                new File(partitionContext.getPenroseContext().getHome(), "conf") :
                new File(path, "DIR-INF");
        File file = new File(dir, config);

        if (file.exists()) {
            log.debug("Loading "+file);
            reader.read(file, federationConfig);
        }

        PartitionManager partitionManager = partitionContext.getPartitionManager();
        partitionsDir = partitionManager.getPartitionsDir();

        for (String name : getPartitionNames()) {
            createPartition(name);
            partitionManager.getQueue().add(name);
        }
    }

    public FederationConfig getFederationConfig() throws Exception {
        return federationConfig;
    }

    public void setFederationConfig(FederationConfig federationConfig) throws Exception {
        this.federationConfig.clear();
        this.federationConfig.copy(federationConfig);
    }

    public Collection<String> getRepositoryTypes() {
        return repositoryTypes;
    }

    public Collection<String> getRepositoryNames() throws Exception {
        return federationConfig.getRepositoryNames();
    }

    public Collection<String> getRepositoryNames(String type) throws Exception {
        return federationConfig.getRepositoryNames(type);
    }

    public Collection<FederationRepositoryConfig> getRepositories() throws Exception {
        return federationConfig.getRepositories();
    }

    public Collection<FederationRepositoryConfig> getRepositories(String type) throws Exception {
        return federationConfig.getRepositories(type);
    }

    public FederationRepositoryConfig getRepository(String name) throws Exception {
        return federationConfig.getRepository(name);
    }

    public Collection<String> getPartitionNames() throws Exception {
        return federationConfig.getPartitionNames();
    }

    public Collection<FederationPartitionConfig> getPartitions() throws Exception {
        return federationConfig.getPartitions();
    }

    public FederationPartitionConfig getPartition(String name) throws Exception {
        return federationConfig.getPartition(name);
    }

    public void createPartition(String partitionName) throws Exception {

        FederationPartitionConfig partitionConfig = federationConfig.getPartition(partitionName);
        if (partitionConfig == null) {
            log.debug(partitionName+" partition undefined.");
            return;
        }

        File partitionDir = new File(partitionsDir, partitionName);

        if (partitionDir.exists()) {
            log.debug(partitionName+" partition already exists.");
            return;
        }

        String template = partitionConfig.getTemplate();
        File templateDir = new File(template);

        log.debug("Creating "+partitionName+" partition from "+template+".");

        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();

        for (String refName : partitionConfig.getRepositoryRefNames()) {
            String repositoryName = partitionConfig.getRepository(refName);
            log.debug(" - Repository "+refName+": "+repositoryName);

            antProject.setProperty(refName+".name", repositoryName);

            FederationRepositoryConfig repositoryConfig = federationConfig.getRepository(repositoryName);

            for (String paramName : repositoryConfig.getParameterNames()) {
                String paramValue = repositoryConfig.getParameter(paramName);
                log.debug("   - "+paramName+": "+paramValue);

                antProject.setProperty(refName+"."+paramName, paramValue);
            }
        }

        for (String paramName : partitionConfig.getParameterNames()) {
            String paramValue = partitionConfig.getParameter(paramName);
            log.debug(" - "+paramName+": "+paramValue);

            antProject.setProperty(paramName, paramValue);
        }

        Copy copy = new Copy();
        copy.setOverwrite(true);
        copy.setProject(antProject);

        FileSet fs = new FileSet();
        fs.setDir(templateDir);
        fs.setIncludes("**/*");
        copy.addFileset(fs);

        copy.setTodir(partitionDir);

        FilterChain filterChain = copy.createFilterChain();
        ExpandProperties expandProperties = new ExpandProperties();
        expandProperties.setProject(antProject);
        filterChain.addExpandProperties(expandProperties);

        copy.execute();
    }

    public void startPartitions() throws Exception {

    }

    public void startPartition(String name) throws Exception {

    }

    public void stopPartitions() throws Exception {

    }

    public void stopPartition(String name) throws Exception {

    }

    public void removePartitions() throws Exception {

    }

    public void removePartition(String name) throws Exception {

    }

    public void synchronize(String name) throws Exception {

    }

    public Collection<String> getConflictDetections() {
        return conflictDetections;
    }
}
