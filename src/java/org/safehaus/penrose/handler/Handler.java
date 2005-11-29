/**
 * Copyright (c) 2000-2005, Identyx Corporation.
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
package org.safehaus.penrose.handler;

import org.safehaus.penrose.SearchResults;
import org.safehaus.penrose.*;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.acl.ACLEngine;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.partition.PartitionManager;
import org.safehaus.penrose.schema.Schema;
import org.safehaus.penrose.engine.Engine;
import org.safehaus.penrose.interpreter.InterpreterFactory;
import org.safehaus.penrose.module.Module;
import org.safehaus.penrose.module.ModuleMapping;
import org.safehaus.penrose.module.ModuleConfig;
import org.safehaus.penrose.module.ModuleContext;
import org.safehaus.penrose.event.*;
import org.apache.log4j.Logger;
import org.ietf.ldap.LDAPEntry;
import org.ietf.ldap.LDAPException;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class Handler implements ModuleContext {

    Logger log = Logger.getLogger(getClass());

    private AddHandler addHandler;
    private BindHandler bindHandler;
    private CompareHandler compareHandler;
    private DeleteHandler deleteHandler;
    private ModifyHandler modifyHandler;
    private ModRdnHandler modRdnHandler;
    private SearchHandler searchHandler;

    private Schema schema;
    private Engine engine;

    private PartitionManager partitionManager;
    private Map modules = new LinkedHashMap();

    private String rootDn;
    private String rootPassword;

    private InterpreterFactory interpreterFactory;
    private ACLEngine aclEngine;
    private FilterTool filterTool;

    public Handler() throws Exception {

    }

    public void init() throws Exception {

        aclEngine = new ACLEngine(this);

        filterTool = new FilterTool();
        filterTool.setSchema(schema);

        addHandler = new AddHandler(this);
        bindHandler = new BindHandler(this);
        compareHandler = new CompareHandler(this);
        deleteHandler = new DeleteHandler(this);
        modifyHandler = new ModifyHandler(this);
        modRdnHandler = new ModRdnHandler(this);
        searchHandler = new SearchHandler(this);

        for (Iterator i=partitionManager.getPartitions().iterator(); i.hasNext(); ) {
            Partition partition = (Partition)i.next();

            for (Iterator j=partition.getModuleConfigs().iterator(); j.hasNext(); ) {
                ModuleConfig moduleConfig = (ModuleConfig)j.next();

                Class clazz = Class.forName(moduleConfig.getModuleClass());
                Module module = (Module)clazz.newInstance();
                module.init(moduleConfig);

                modules.put(moduleConfig.getModuleName(), module);
            }
        }
    }

    public int add(PenroseConnection connection, LDAPEntry entry) throws Exception {
        return getAddHandler().add(connection, entry);
    }

    public int bind(PenroseConnection connection, String dn, String password) throws Exception {
        return getBindHandler().bind(connection, dn, password);
    }

    public int compare(PenroseConnection connection, String dn, String attributeName,
            String attributeValue) throws Exception {

        return getCompareHandler().compare(connection, dn, attributeName, attributeValue);
    }

    public int unbind(PenroseConnection connection) throws Exception {
        return getBindHandler().unbind(connection);
    }

    public int delete(PenroseConnection connection, String dn) throws Exception {
        return getDeleteHandler().delete(connection, dn);
    }

    public int modify(PenroseConnection connection, String dn, List modifications) throws Exception {
        return getModifyHandler().modify(connection, dn, modifications);
    }

    public int modrdn(PenroseConnection connection, String dn, String newRdn) throws Exception {
        return getModRdnHandler().modrdn(connection, dn, newRdn);
    }

    public SearchResults search(
            final PenroseConnection connection,
            final String base,
            final int scope,
            final int deref,
            final String filter,
            final Collection attributeNames)
            throws Exception {

        final SearchResults results = new SearchResults();

        //getSearchHandler().search(connection, base, scope, deref, filter, attributeNames, results);

        engine.execute(new Runnable() {
            public void run() {
                try {
                    getSearchHandler().search(connection, base, scope, deref, filter, attributeNames, results);

                } catch (Throwable e) {
                    e.printStackTrace(System.out);
                    results.setReturnCode(LDAPException.OPERATIONS_ERROR);
                    results.close();
                }
            }
        });

        return results;
    }

    public BindHandler getBindHandler() {
        return bindHandler;
    }

    public void setBindHandler(BindHandler bindHandler) {
        this.bindHandler = bindHandler;
    }

    public SearchHandler getSearchHandler() {
        return searchHandler;
    }

    public void setSearchHandler(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    public AddHandler getAddHandler() {
        return addHandler;
    }

    public void setAddHandler(AddHandler addHandler) {
        this.addHandler = addHandler;
    }

    public ModifyHandler getModifyHandler() {
        return modifyHandler;
    }

    public void setModifyHandler(ModifyHandler modifyHandler) {
        this.modifyHandler = modifyHandler;
    }

    public DeleteHandler getDeleteHandler() {
        return deleteHandler;
    }

    public void setDeleteHandler(DeleteHandler deleteHandler) {
        this.deleteHandler = deleteHandler;
    }

    public CompareHandler getCompareHandler() {
        return compareHandler;
    }

    public void setCompareHandler(CompareHandler compareHandler) {
        this.compareHandler = compareHandler;
    }

    public ModRdnHandler getModRdnHandler() {
        return modRdnHandler;
    }

    public void setModRdnHandler(ModRdnHandler modRdnHandler) {
        this.modRdnHandler = modRdnHandler;
    }

    public void postEvent(String dn, Event event) throws Exception {
        Collection c = getModules(dn);

        for (Iterator i=c.iterator(); i.hasNext(); ) {
            Module module = (Module)i.next();

            if (event instanceof AddEvent) {
                switch (event.getType()) {
                    case AddEvent.BEFORE_ADD:
                        module.beforeAdd((AddEvent)event);
                        break;

                    case AddEvent.AFTER_ADD:
                        module.afterAdd((AddEvent)event);
                        break;
                }

            } else if (event instanceof BindEvent) {

                switch (event.getType()) {
                    case BindEvent.BEFORE_BIND:
                        module.beforeBind((BindEvent)event);
                        break;

                    case BindEvent.AFTER_BIND:
                        module.afterBind((BindEvent)event);
                        break;
                }

            } else if (event instanceof DeleteEvent) {

                switch (event.getType()) {
                    case DeleteEvent.BEFORE_DELETE:
                        module.beforeDelete((DeleteEvent)event);
                        break;

                    case DeleteEvent.AFTER_DELETE:
                        module.afterDelete((DeleteEvent)event);
                        break;
                }

            } else if (event instanceof ModifyEvent) {

                switch (event.getType()) {
                case ModifyEvent.BEFORE_MODIFY:
                    module.beforeModify((ModifyEvent)event);
                    break;

                case ModifyEvent.AFTER_MODIFY:
                    module.afterModify((ModifyEvent)event);
                    break;
                }

            } else if (event instanceof SearchEvent) {

                switch (event.getType()) {
                    case SearchEvent.BEFORE_SEARCH:
                        module.beforeSearch((SearchEvent)event);
                        break;

                    case SearchEvent.AFTER_SEARCH:
                        module.afterSearch((SearchEvent)event);
                        break;
                }

            }
        }
    }

    public InterpreterFactory getInterpreterFactory() {
        return interpreterFactory;
    }

    public void setInterpreterFactory(InterpreterFactory interpreterFactory) {
        this.interpreterFactory = interpreterFactory;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Collection getModules(String dn) throws Exception {
        log.debug("Find matching module mapping for "+dn);

        Collection list = new ArrayList();

        Partition partition = engine.getPartitionManager().getConfig(dn);
        if (partition == null) return list;

        for (Iterator i = partition.getModuleMappings().iterator(); i.hasNext(); ) {
            Collection c = (Collection)i.next();

            for (Iterator j=c.iterator(); j.hasNext(); ) {
                ModuleMapping moduleMapping = (ModuleMapping)j.next();

                String moduleName = moduleMapping.getModuleName();
                Module module = (Module)modules.get(moduleName);

                if (moduleMapping.match(dn)) {
                    log.debug(" - "+moduleName);
                    list.add(module);
                }
            }
        }

        return list;
    }

    public String getRootDn() {
        return rootDn;
    }

    public void setRootDn(String rootDn) {
        this.rootDn = rootDn;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public PartitionManager getConfigManager() {
        return partitionManager;
    }

    public void setConfigManager(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public FilterTool getFilterTool() {
        return filterTool;
    }

    public void setFilterTool(FilterTool filterTool) {
        this.filterTool = filterTool;
    }

    public ACLEngine getACLEngine() {
        return aclEngine;
    }

    public void setACLEngine(ACLEngine aclEngine) {
        this.aclEngine = aclEngine;
    }

    // ------------------------------------------------
    // Listeners
    // ------------------------------------------------

    public void addConnectionListener(ConnectionListener l) {
    }

    public void removeConnectionListener(ConnectionListener l) {
    }

    public void addBindListener(BindListener l) {
    }

    public void removeBindListener(BindListener l) {
    }

    public void addSearchListener(SearchListener l) {
    }

    public void removeSearchListener(SearchListener l) {
    }

    public void addCompareListener(CompareListener l) {
    }

    public void removeCompareListener(CompareListener l) {
    }

    public void addAddListener(AddListener l) {
    }

    public void removeAddListener(AddListener l) {
    }

    public void addDeleteListener(DeleteListener l) {
    }

    public void removeDeleteListener(DeleteListener l) {
    }

    public void addModifyListener(ModifyListener l) {
    }

    public void removeModifyListener(ModifyListener l) {
    }
}

