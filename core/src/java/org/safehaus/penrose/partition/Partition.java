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
package org.safehaus.penrose.partition;

import java.util.*;

import org.safehaus.penrose.module.ModuleMapping;
import org.safehaus.penrose.module.ModuleConfig;
import org.safehaus.penrose.mapping.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Endi S. Dewata
 */
public class Partition {

    Logger log = LoggerFactory.getLogger(getClass());

    private PartitionConfig partitionConfig;

    private Map entryMappings = new LinkedHashMap();
    private Collection rootEntryMappings = new ArrayList();
    private Map childrenMap = new LinkedHashMap();

    private Map connectionConfigs = new LinkedHashMap();
    private Map sourceConfigs = new LinkedHashMap();

    private Map moduleConfigs = new LinkedHashMap();
    private Map moduleMappings = new LinkedHashMap();

    public Partition(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }

    public String getName() {
        return partitionConfig.getName();
    }

    public boolean containsEntryMapping(EntryMapping entryMapping) {
        return entryMappings.containsKey(entryMapping.getDn().toLowerCase());
    }

    public Collection getEntryMappings(String dn) {
        if (dn == null) return null;

        Collection list = (Collection)entryMappings.get(dn.toLowerCase());
        if (list == null) return null;

        return new ArrayList(list);
    }

    public Collection getEntryMappings(SourceConfig sourceConfig) {
        Collection list = new ArrayList();

        for (Iterator i=rootEntryMappings.iterator(); i.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping)i.next();
            getEntryMappings(entryMapping, sourceConfig, list);
        }

        return list;
    }

    public void getEntryMappings(EntryMapping entryMapping, SourceConfig sourceConfig, Collection list) {

        //log.debug("Checking "+entryMapping.getDn());

        Collection sourceMappings = entryMapping.getSourceMappings();
        for (Iterator i=sourceMappings.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();
            if (sourceMapping.getSourceName().equals(sourceConfig.getName())) {
                list.add(entryMapping);
                return;
            }
        }

        Collection children = getChildren(entryMapping);
        for (Iterator i=children.iterator(); i.hasNext(); ) {
            EntryMapping childMapping = (EntryMapping)i.next();
            getEntryMappings(childMapping, sourceConfig, list);
        }
    }

    public void addEntryMapping(EntryMapping entryMapping) throws Exception {

        String dn = entryMapping.getDn();
        log.debug("Adding entry "+dn);

        Collection c = (Collection)entryMappings.get(dn.toLowerCase());
        if (c == null) {
            c = new ArrayList();
            entryMappings.put(dn.toLowerCase(), c);
        }
        //if (entryMappings.get(dn) != null) throw new Exception("Entry "+dn+" already exists.");
        c.add(entryMapping);


        EntryMapping parent = getParent(entryMapping);

        if (parent != null) { // parent found
            //System.out.println("Found parent "+parentDn+".");
            addChildren(parent, entryMapping);
        } else {
            rootEntryMappings.add(entryMapping);
        }

        //entryMappings.put(dn, entryMapping);
    }

    public void modifyEntryMapping(EntryMapping oldEntry, EntryMapping newEntry) {
        oldEntry.copy(newEntry);
    }

    public void removeEntryMapping(EntryMapping entryMapping) {
        EntryMapping parent = getParent(entryMapping);
        if (parent == null) {
            rootEntryMappings.remove(entryMapping);

        } else {
            Collection children = getChildren(parent);
            if (children != null) children.remove(entryMapping);
        }

        Collection c = (Collection)entryMappings.get(entryMapping.getDn().toLowerCase());
        if (c == null) return;

        c.remove(entryMapping);
        if (c.isEmpty()) {
            entryMappings.remove(entryMapping.getDn().toLowerCase());
        }
    }

    public void renameEntryMapping(EntryMapping entryMapping, String newDn) {
        if (entryMapping == null) return;
        if (entryMapping.getDn().equals(newDn)) return;

        EntryMapping oldParent = getParent(entryMapping);
        String oldDn = entryMapping.getDn();

        log.debug("Renaming "+oldDn+" to "+newDn);

        Collection c = (Collection)entryMappings.get(oldDn.toLowerCase());
        if (c == null) {
            log.debug("Entry "+oldDn+" not found.");
            return;
        }

        c.remove(entryMapping);
        if (c.isEmpty()) {
            log.debug("Last "+oldDn);
            entryMappings.remove(oldDn.toLowerCase());
        }

        entryMapping.setDn(newDn);
        Collection newList = (Collection)entryMappings.get(newDn.toLowerCase());
        if (newList == null) {
            log.debug("First "+newDn);
            newList = new ArrayList();
            entryMappings.put(newDn.toLowerCase(), newList);
        }
        newList.add(entryMapping);

        EntryMapping newParent = getParent(entryMapping);
        log.debug("New parent "+(newParent == null ? null : newParent.getDn()));

        if (newParent != null) {
            addChildren(newParent, entryMapping);
        }

        Collection children = getChildren(entryMapping);

        if (children != null) {
            //addChildren(newDn, children);

            for (Iterator i=children.iterator(); i.hasNext(); ) {
                EntryMapping child = (EntryMapping)i.next();
                String childNewDn = child.getRdn()+","+newDn;
                //System.out.println(" - renaming child "+child.getDn()+" to "+childNewDn);

                renameChildren(child, childNewDn);
            }

            //removeChildren(oldDn);
        }

        if (oldParent != null) {
            Collection oldSiblings = getChildren(oldParent);
            if (oldSiblings != null) oldSiblings.remove(entryMapping);
        }

    }

    public void renameChildren(EntryMapping entryMapping, String newDn) {
        if (entryMapping == null) return;
        if (newDn.equals(entryMapping.getDn())) return;

        String oldDn = entryMapping.getDn();
        log.debug("Renaming "+oldDn+" to "+newDn);

        Collection c = getEntryMappings(oldDn);
        if (c == null) return;

        c.remove(entryMapping);
        if (c.isEmpty()) {
            log.debug("Last "+oldDn);
            entryMappings.remove(oldDn.toLowerCase());
        }

        entryMapping.setDn(newDn);
        Collection newList = (Collection)entryMappings.get(newDn.toLowerCase());
        if (newList == null) {
            log.debug("First "+newDn);
            newList = new ArrayList();
            entryMappings.put(newDn.toLowerCase(), newList);
        }
        newList.add(entryMapping);

        Collection children = getChildren(entryMapping);

        if (children != null) {
            //addChildren(newDn, children);

            for (Iterator i=children.iterator(); i.hasNext(); ) {
                EntryMapping child = (EntryMapping)i.next();
                String childNewDn = child.getRdn()+","+newDn;
                //System.out.println(" - renaming child "+child.getDn()+" to "+childNewDn);

                renameChildren(child, childNewDn);
            }

            //removeChildren(oldDn);
        }
    }

    public EntryMapping getParent(EntryMapping entryMapping) {
        if (entryMapping == null) return null;

        String parentDn = entryMapping.getParentDn();
        if (parentDn == null) return null;

        Collection c = getEntryMappings(parentDn);
        if (c == null || c.isEmpty()) return null;

        return (EntryMapping)c.iterator().next();
    }

    public Collection getChildren(EntryMapping entryMapping) {
        Collection children = (Collection)childrenMap.get(entryMapping);
        if (children == null) return new ArrayList();
        return children;
    }

    public void addChildren(EntryMapping parentMapping, Collection newChildren) {
        Collection children = (Collection)childrenMap.get(parentMapping);
        if (children == null) {
            children = new ArrayList();
            childrenMap.put(parentMapping, children);
        }
        for (Iterator i=newChildren.iterator(); i.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping)i.next();
            children.add(entryMapping);
        }
    }

    public void addChildren(EntryMapping parentMapping, EntryMapping entryMapping) {
        Collection children = (Collection)childrenMap.get(parentMapping);
        if (children == null) {
            children = new ArrayList();
            childrenMap.put(parentMapping, children);
        }
        children.add(entryMapping);
    }

    public Collection removeChildren(EntryMapping entryMapping) {
        return (Collection)childrenMap.remove(entryMapping);
    }

   public Collection getEffectiveSourceMappings(EntryMapping entryMapping) {
        Collection list = new ArrayList();
        list.addAll(entryMapping.getSourceMappings());

        EntryMapping parent = getParent(entryMapping);
        if (parent != null) list.addAll(getEffectiveSourceMappings(parent));

        return list;
    }

    public SourceMapping getEffectiveSourceMapping(EntryMapping entryMapping, String name) {
        SourceMapping sourceMapping = (SourceMapping)entryMapping.getSourceMapping(name);
        if (sourceMapping != null) return sourceMapping;

        EntryMapping parent = getParent(entryMapping);
        if (parent != null) return getEffectiveSourceMapping(parent, name);

        return null;
    }

    public Collection getEffectiveRelationships(EntryMapping entryMapping) {
        Collection relationships = new ArrayList();
        relationships.addAll(entryMapping.getRelationships());

        EntryMapping parent = getParent(entryMapping);
        if (parent != null) relationships.addAll(getEffectiveRelationships(parent));

        return relationships;
    }

    public void addModuleConfig(ModuleConfig moduleConfig) throws Exception {
        moduleConfigs.put(moduleConfig.getName(), moduleConfig);
    }

    public ModuleConfig getModuleConfig(String name) {
        return (ModuleConfig)moduleConfigs.get(name);
    }

    public Collection getModuleMappings(String name) {
        return (Collection)moduleMappings.get(name);
    }

    public void addModuleMapping(ModuleMapping mapping) throws Exception {
        Collection c = (Collection)moduleMappings.get(mapping.getModuleName());
        if (c == null) {
            c = new ArrayList();
            moduleMappings.put(mapping.getModuleName(), c);
        }
        c.add(mapping);

        String moduleName = mapping.getModuleName();
        if (moduleName == null) throw new Exception("Missing module name");

        ModuleConfig moduleConfig = getModuleConfig(moduleName);
        if (moduleConfig == null) throw new Exception("Undefined module "+moduleName);

        mapping.setModuleConfig(moduleConfig);
    }

    public void addConnectionConfig(ConnectionConfig connectionConfig) {
        connectionConfigs.put(connectionConfig.getName(), connectionConfig);
    }

    public void renameConnectionConfig(ConnectionConfig connectionConfig, String newName) {
        if (connectionConfig == null) return;
        if (connectionConfig.getName().equals(newName)) return;

        connectionConfigs.remove(connectionConfig.getName());
        connectionConfigs.put(newName, connectionConfig);
    }

    public void modifyConnectionConfig(String name, ConnectionConfig newConnectionConfig) {
        ConnectionConfig connectionConfig = (ConnectionConfig)connectionConfigs.get(name);
        connectionConfig.copy(newConnectionConfig);
    }

    public ConnectionConfig removeConnectionConfig(String connectionName) {
        return (ConnectionConfig)connectionConfigs.remove(connectionName);
    }

    public ConnectionConfig getConnectionConfig(String name) {
        return (ConnectionConfig)connectionConfigs.get(name);
    }

    public void addSourceConfig(SourceConfig sourceConfig) {
        log.debug("Adding source "+sourceConfig.getName());
        sourceConfigs.put(sourceConfig.getName(), sourceConfig);
    }

    public SourceConfig removeSourceConfig(String name) {
        return (SourceConfig)sourceConfigs.remove(name);
    }

    public SourceConfig getSourceConfig(String name) {
        return (SourceConfig)sourceConfigs.get(name);
    }

    public Collection getSourceConfigs() {
        return sourceConfigs.values();
    }

    public void renameSourceConfig(SourceConfig sourceConfig, String newName) {
        if (sourceConfig == null) return;
        if (sourceConfig.getName().equals(newName)) return;

        sourceConfigs.remove(sourceConfig.getName());
        sourceConfigs.put(newName, sourceConfig);
    }

    public void modifySourceConfig(String name, SourceConfig newSourceConfig) {
        SourceConfig sourceConfig = (SourceConfig)sourceConfigs.get(name);
        sourceConfig.copy(newSourceConfig);
    }

    public Collection getEntryMappings() {
        Collection list = new ArrayList();
        for (Iterator i=entryMappings.values().iterator(); i.hasNext(); ) {
            Collection c = (Collection)i.next();
            list.addAll(c);
        }
        return list;
    }

    public Collection getConnectionConfigs() {
        return connectionConfigs.values();
    }

    public void setConnectionConfigs(Map connectionConfigs) {
        this.connectionConfigs = connectionConfigs;
    }

    public Collection getModuleMappings() {
        return moduleMappings.values();
    }
    public Collection getRootEntryMappings() {
        return rootEntryMappings;
    }

    public ModuleConfig removeModuleConfig(String moduleName) {
        return (ModuleConfig)moduleConfigs.remove(moduleName);
    }

    public Collection getModuleConfigs() {
        return moduleConfigs.values();
    }

    public Collection removeModuleMapping(String moduleName) {
        return (Collection)moduleMappings.remove(moduleName);
    }

    public void removeModuleMapping(ModuleMapping mapping) {
        if (mapping == null) return;
        if (mapping.getModuleName() == null) return;

        Collection c = (Collection)moduleMappings.get(mapping.getModuleName());
        if (c != null) c.remove(mapping);
    }

    public void setModuleConfigs(Map moduleConfigs) {
        this.moduleConfigs = moduleConfigs;
    }

    public void setModuleMappings(Map moduleMappings) {
        this.moduleMappings = moduleMappings;
    }

    public void setRootEntryMappings(Collection rootEntryMappings) {
        this.rootEntryMappings = rootEntryMappings;
    }

    public Collection getSearchableFields(SourceMapping sourceMapping) {
        SourceConfig sourceConfig = getSourceConfig(sourceMapping.getSourceName());

        Collection results = new ArrayList();
        for (Iterator i=sourceMapping.getFieldMappings().iterator(); i.hasNext(); ) {
            FieldMapping fieldMapping = (FieldMapping)i.next();
            FieldConfig fieldConfig = sourceConfig.getFieldConfig(fieldMapping.getName());
            if (fieldConfig == null) continue;
            if (!fieldConfig.isSearchable()) continue;
            results.add(fieldMapping);
        }

        return results;
    }

    public boolean isDynamic(EntryMapping entryMapping) {

        boolean dynamic = entryMapping.isDynamic();

        //log.debug("Mapping "+entryMapping.getDn()+" is "+(dynamic ? "dynamic" : "not dynamic"));
        if (dynamic) return true;

        EntryMapping parentMapping = getParent(entryMapping);
        if (parentMapping == null) return false;

        return isDynamic(parentMapping);
    }

    public PartitionConfig getPartitionConfig() {
        return partitionConfig;
    }

    public void setPartitionConfig(PartitionConfig partitionConfig) {
        this.partitionConfig = partitionConfig;
    }

    public boolean isProxy(EntryMapping entryMapping) {
        Collection sourceMappings = entryMapping.getSourceMappings();
        if (sourceMappings.size() != 1) return false;

        SourceMapping sourceMapping = (SourceMapping)sourceMappings.iterator().next();
        return "PROXY".equals(sourceMapping.getEngine());
    }
}
