package org.safehaus.penrose.management.module;

import org.safehaus.penrose.module.Module;
import org.safehaus.penrose.module.ModuleConfig;
import org.safehaus.penrose.module.ModuleConfigManager;
import org.safehaus.penrose.module.ModuleMapping;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.PartitionConfig;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.management.BaseService;
import org.safehaus.penrose.management.PenroseJMXService;

import java.util.Collection;

/**
 * @author Endi Sukma Dewata
 */
public class ModuleService extends BaseService implements ModuleServiceMBean {

    private PartitionManager partitionManager;
    private String partitionName;
    private String moduleName;

    public ModuleService(
            PenroseJMXService jmxService,
            PartitionManager partitionManager,
            String partitionName,
            String moduleName
    ) throws Exception {
        super(ModuleServiceMBean.class);

        this.jmxService = jmxService;
        this.partitionManager = partitionManager;
        this.partitionName = partitionName;
        this.moduleName = moduleName;
    }

    public String getObjectName() {
        return ModuleClient.getStringObjectName(partitionName, moduleName);
    }

    public Object getObject() {
        return getModule();
    }

    public ModuleConfig getModuleConfig() throws Exception {
        return getPartitionConfig().getModuleConfigManager().getModuleConfig(moduleName);
    }

    public Module getModule() {
        return getPartition().getModule(moduleName);
    }

    public PartitionConfig getPartitionConfig() {
        return partitionManager.getPartitionConfig(partitionName);
    }

    public Partition getPartition() {
        return partitionManager.getPartition(partitionName);
    }

    public void addModuleMapping(ModuleMapping moduleMapping) throws Exception {
        PartitionConfig partitionConfig = getPartitionConfig();
        ModuleConfigManager moduleConfigManager = partitionConfig.getModuleConfigManager();
        moduleConfigManager.addModuleMapping(moduleMapping);
    }

    public void removeModuleMapping(ModuleMapping moduleMapping) throws Exception {
        PartitionConfig partitionConfig = getPartitionConfig();
        ModuleConfigManager moduleConfigManager = partitionConfig.getModuleConfigManager();
        moduleConfigManager.removeModuleMapping(moduleMapping);
    }

    public Collection<ModuleMapping> getModuleMappings() throws Exception {
        PartitionConfig partitionConfig = getPartitionConfig();
        ModuleConfigManager moduleConfigManager = partitionConfig.getModuleConfigManager();
        return moduleConfigManager.getModuleMappings(moduleName);
    }

    public void start() throws Exception {

        log.debug("Starting module "+partitionName+"/"+moduleName+"...");

        Module module = getModule();
        module.init();

        log.debug("Module started.");
    }

    public void stop() throws Exception {

        log.debug("Stopping module "+partitionName+"/"+moduleName+"...");

        Module module = getModule();
        module.destroy();

        log.debug("Module stopped.");
    }

    public void restart() throws Exception {

        log.debug("Restarting module "+partitionName+"/"+moduleName+"...");

        Module module = getModule();
        module.destroy();
        module.init();

        log.debug("Module restarted.");
    }
}