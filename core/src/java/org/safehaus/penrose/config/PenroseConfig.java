/**
 * Copyright (c) 2000-2006, Identyx Corporation.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.safehaus.penrose.config;

import java.util.*;

import org.safehaus.penrose.cache.CacheConfig;
import org.safehaus.penrose.cache.EntryCache;
import org.safehaus.penrose.engine.EngineConfig;
import org.safehaus.penrose.engine.simple.SimpleEngine;
import org.safehaus.penrose.interpreter.InterpreterConfig;
import org.safehaus.penrose.interpreter.DefaultInterpreter;
import org.safehaus.penrose.connector.ConnectorConfig;
import org.safehaus.penrose.adapter.AdapterConfig;
import org.safehaus.penrose.partition.PartitionConfig;
import org.safehaus.penrose.schema.SchemaConfig;
import org.safehaus.penrose.user.UserConfig;
import org.safehaus.penrose.session.SessionConfig;
import org.safehaus.penrose.handler.HandlerConfig;
import org.safehaus.penrose.handler.DefaultHandler;
import org.safehaus.penrose.service.ServiceConfig;
import org.safehaus.penrose.entry.DN;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Endi S. Dewata
 */
public class PenroseConfig implements PenroseConfigMBean, Cloneable {

    Logger log = LoggerFactory.getLogger(getClass());

    private String home;

    private Map systemProperties   = new LinkedHashMap();
    private Map properties         = new LinkedHashMap();
    private Map serviceConfigs     = new LinkedHashMap();

    private Map schemaConfigs      = new LinkedHashMap();
    private Map adapterConfigs     = new LinkedHashMap();
    private Map partitionConfigs   = new LinkedHashMap();
    private Map engineConfigs      = new LinkedHashMap();
    private Map handlerConfigs     = new LinkedHashMap();
    private Map interpreterConfigs = new LinkedHashMap();

    private CacheConfig entryCacheConfig;
    private CacheConfig sourceCacheConfig;

    private SessionConfig sessionConfig;
    private ConnectorConfig connectorConfig;

    private UserConfig rootUserConfig;

    public PenroseConfig() {

        sourceCacheConfig = new CacheConfig();
        sourceCacheConfig.setName(ConnectorConfig.DEFAULT_CACHE_NAME);
        sourceCacheConfig.setCacheClass(ConnectorConfig.DEFAULT_CACHE_CLASS);

        entryCacheConfig = new CacheConfig();
        entryCacheConfig.setName(EntryCache.DEFAULT_CACHE_NAME);
        entryCacheConfig.setCacheClass(EntryCache.DEFAULT_CACHE_CLASS);

        connectorConfig = new ConnectorConfig();
        sessionConfig = new SessionConfig();

        addInterpreterConfig(new InterpreterConfig("DEFAULT", DefaultInterpreter.class.getName()));
        addHandlerConfig(new HandlerConfig("DEFAULT", DefaultHandler.class.getName()));
        addEngineConfig(new EngineConfig("DEFAULT", SimpleEngine.class.getName()));

        rootUserConfig = new UserConfig("uid=admin,ou=system", "secret");
    }

    public String getSystemProperty(String name) {
        return (String)systemProperties.get(name);
    }

    public Map getSystemProperties() {
        return systemProperties;
    }

    public Collection getSystemPropertyNames() {
        return systemProperties.keySet();
    }

    public void setSystemProperty(String name, String value) {
        systemProperties.put(name, value);
    }

    public String removeSystemProperty(String name) {
        return (String)systemProperties.remove(name);
    }

    public String getProperty(String name) {
        return (String)properties.get(name);
    }

    public Map getProperties() {
        return properties;
    }

    public Collection getPropertyNames() {
        return properties.keySet();
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public String removeProperty(String name) {
        return (String)properties.remove(name);
    }

    public void addServiceConfig(ServiceConfig serviceConfig) {
        serviceConfigs.put(serviceConfig.getName(), serviceConfig);
    }

    public ServiceConfig getServiceConfig(String name) {
        return (ServiceConfig)serviceConfigs.get(name);
    }

    public Collection getServiceConfigs() {
        return serviceConfigs.values();
    }

    public Collection getServiceNames() {
        return serviceConfigs.keySet();
    }

    public ServiceConfig removeServiceConfig(String name) {
        return (ServiceConfig)serviceConfigs.remove(name);
    }

    public void addEngineConfig(EngineConfig engineConfig) {
        engineConfigs.put(engineConfig.getName(), engineConfig);
    }

    public EngineConfig getEngineConfig(String name) {
        return (EngineConfig)engineConfigs.get(name);
    }

    public Collection getEngineConfigs() {
        return engineConfigs.values();
    }

    public Collection getEngineNames() {
        return engineConfigs.keySet();
    }

    public Collection getInterpreterConfigs() {
        return interpreterConfigs.values();
    }

    public void addInterpreterConfig(InterpreterConfig interpreterConfig) {
        interpreterConfigs.put(interpreterConfig.getName(), interpreterConfig);
    }

    public Collection getAdapterConfigs() {
        return adapterConfigs.values();
    }

    public AdapterConfig getAdapterConfig(String name) {
        return (AdapterConfig)adapterConfigs.get(name);
    }

    public void setAdapterConfigs(Map adapterConfigs) {
        this.adapterConfigs = adapterConfigs;
    }

    public void addAdapterConfig(AdapterConfig adapter) {
        adapterConfigs.put(adapter.getName(), adapter);
    }

    public Collection getAdapterNames() {
        return adapterConfigs.keySet();
    }

    public void setConnectorConfig(ConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    public ConnectorConfig getConnectorConfig() {
        return connectorConfig;
    }

    public CacheConfig getEntryCacheConfig() {
        return entryCacheConfig;
    }

    public void setEntryCacheConfig(CacheConfig entryCacheConfig) {
        this.entryCacheConfig = entryCacheConfig;
    }

    public CacheConfig getSourceCacheConfig() {
        return sourceCacheConfig;
    }

    public void setSourceCacheConfig(CacheConfig sourceCacheConfig) {
        this.sourceCacheConfig = sourceCacheConfig;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public void addSchemaConfig(SchemaConfig schemaConfig) {
        schemaConfigs.put(schemaConfig.getName(), schemaConfig);
    }

    public SchemaConfig getSchemaConfig(String name) {
        return (SchemaConfig)schemaConfigs.get(name);
    }

    public Collection getSchemaConfigs() {
        return schemaConfigs.values();
    }

    public Collection getSchemaNames() {
        return schemaConfigs.keySet();
    }

    public SchemaConfig removeSchemaConfig(String name) {
        return (SchemaConfig)schemaConfigs.remove(name);
    }

    public void addPartitionConfig(PartitionConfig partitionConfig) {
        partitionConfigs.put(partitionConfig.getName(), partitionConfig);
    }

    public PartitionConfig getPartitionConfig(String name) {
        return (PartitionConfig)partitionConfigs.get(name);
    }

    public Collection getPartitionConfigs() {
        return partitionConfigs.values();
    }

    public Collection getPartitionNames() {
        return partitionConfigs.keySet();
    }

    public PartitionConfig removePartitionConfig(String name) {
        return (PartitionConfig)partitionConfigs.remove(name);
    }

    public HandlerConfig getHandlerConfig(String name) {
        return (HandlerConfig)handlerConfigs.get(name);
    }

    public Collection getHandlerConfigs() {
        return handlerConfigs.values();
    }

    public Collection getHandlerNames() {
        return handlerConfigs.keySet();
    }

    public void addHandlerConfig(HandlerConfig handlerConfig) {
        log.debug("Adding handler "+handlerConfig.getName()+": "+handlerConfig.getHandlerClass());
        handlerConfigs.put(handlerConfig.getName(), handlerConfig);
    }

    public HandlerConfig removeHandlerConfig(String name) {
        return (HandlerConfig)handlerConfigs.remove(name);
    }

    public UserConfig getRootUserConfig() {
        return rootUserConfig;
    }

    public void setRootUserConfig(UserConfig rootUserConfig) {
        this.rootUserConfig = rootUserConfig;
    }

    public DN getRootDn() {
        return rootUserConfig.getDn();
    }

    public void setRootDn(String rootDn) {
        rootUserConfig.setDn(rootDn);
    }

    public void setRootDn(DN rootDn) {
        rootUserConfig.setDn(rootDn);
    }

    public String getRootPassword() {
        return rootUserConfig.getPassword();
    }

    public void setRootPassword(String rootPassword) {
        rootUserConfig.setPassword(rootPassword);
    }

    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    public int hashCode() {
        return (home == null ? 0 : home.hashCode()) +
                (systemProperties == null ? 0 : systemProperties.hashCode()) +
                (properties == null ? 0 : properties.hashCode()) +
                (serviceConfigs == null ? 0 : serviceConfigs.hashCode()) +
                (schemaConfigs == null ? 0 : schemaConfigs.hashCode()) +
                (adapterConfigs == null ? 0 : adapterConfigs.hashCode()) +
                (partitionConfigs == null ? 0 : partitionConfigs.hashCode()) +
                (handlerConfigs == null ? 0 : handlerConfigs.hashCode()) +
                (interpreterConfigs == null ? 0 : interpreterConfigs.hashCode()) +
                (entryCacheConfig == null ? 0 : entryCacheConfig.hashCode()) +
                (sourceCacheConfig == null ? 0 : sourceCacheConfig.hashCode()) +
                (sessionConfig == null ? 0 : sessionConfig.hashCode()) +
                (engineConfigs == null ? 0 : engineConfigs.hashCode()) +
                (connectorConfig == null ? 0 : connectorConfig.hashCode()) +
                (rootUserConfig == null ? 0 : rootUserConfig.hashCode());
    }

    boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) return true;
        if (o1 != null) return o1.equals(o2);
        return o2.equals(o1);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if((object == null) || (object.getClass() != this.getClass())) return false;

        PenroseConfig penroseConfig = (PenroseConfig)object;

        if (!equals(home, penroseConfig.home)) return false;

        if (!equals(systemProperties, penroseConfig.systemProperties)) return false;
        if (!equals(properties, penroseConfig.properties)) return false;
        if (!equals(serviceConfigs, penroseConfig.serviceConfigs)) return false;

        if (!equals(schemaConfigs, penroseConfig.schemaConfigs)) return false;
        if (!equals(adapterConfigs, penroseConfig.adapterConfigs)) return false;
        if (!equals(partitionConfigs, penroseConfig.partitionConfigs)) return false;
        if (!equals(handlerConfigs, penroseConfig.handlerConfigs)) return false;
        if (!equals(interpreterConfigs, penroseConfig.interpreterConfigs)) return false;

        if (!equals(entryCacheConfig, penroseConfig.entryCacheConfig)) return false;
        if (!equals(sourceCacheConfig, penroseConfig.sourceCacheConfig)) return false;

        if (!equals(sessionConfig, penroseConfig.sessionConfig)) return false;
        if (!equals(engineConfigs, penroseConfig.engineConfigs)) return false;
        if (!equals(connectorConfig, penroseConfig.connectorConfig)) return false;

        if (!equals(rootUserConfig, penroseConfig.rootUserConfig)) return false;

        return true;
    }

    public void copy(PenroseConfig penroseConfig) {
        clear();

        home = penroseConfig.home;

        systemProperties.putAll(penroseConfig.systemProperties);
        properties.putAll(penroseConfig.properties);

        for (Iterator i=penroseConfig.serviceConfigs.values().iterator(); i.hasNext(); ) {
            ServiceConfig serviceConfig = (ServiceConfig)i.next();
            addServiceConfig((ServiceConfig)serviceConfig.clone());
        }

        for (Iterator i=penroseConfig.schemaConfigs.values().iterator(); i.hasNext(); ) {
            SchemaConfig schemaConfig = (SchemaConfig)i.next();
            addSchemaConfig((SchemaConfig)schemaConfig.clone());
        }

        for (Iterator i=penroseConfig.adapterConfigs.values().iterator(); i.hasNext(); ) {
            AdapterConfig adapterConfig = (AdapterConfig)i.next();
            addAdapterConfig((AdapterConfig)adapterConfig.clone());
        }

        for (Iterator i=penroseConfig.partitionConfigs.values().iterator(); i.hasNext(); ) {
            PartitionConfig partitionConfig = (PartitionConfig)i.next();
            addPartitionConfig((PartitionConfig)partitionConfig.clone());
        }

        for (Iterator i=penroseConfig.engineConfigs.values().iterator(); i.hasNext(); ) {
            EngineConfig engineConfig = (EngineConfig)i.next();
            addEngineConfig((EngineConfig)engineConfig.clone());
        }

        for (Iterator i=penroseConfig.handlerConfigs.values().iterator(); i.hasNext(); ) {
            HandlerConfig handlerConfig = (HandlerConfig)i.next();
            addHandlerConfig((HandlerConfig)handlerConfig.clone());
        }

        for (Iterator i=penroseConfig.interpreterConfigs.values().iterator(); i.hasNext(); ) {
            InterpreterConfig interpreterConfig = (InterpreterConfig)i.next();
            addInterpreterConfig((InterpreterConfig)interpreterConfig.clone());
        }

        entryCacheConfig.copy(entryCacheConfig);
        sourceCacheConfig.copy(sourceCacheConfig);

        sessionConfig.copy(sessionConfig);
        connectorConfig.copy(connectorConfig);

        rootUserConfig.copy(rootUserConfig);
    }

    public void clear() {
        systemProperties.clear();
        properties.clear();
        serviceConfigs.clear();
        schemaConfigs.clear();
        adapterConfigs.clear();
        partitionConfigs.clear();
        engineConfigs.clear();
        handlerConfigs.clear();
        interpreterConfigs.clear();
    }

    public Object clone() {
        PenroseConfig penroseConfig = new PenroseConfig();
        penroseConfig.copy(this);
        return penroseConfig;
    }
}
