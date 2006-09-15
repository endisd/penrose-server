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

import org.safehaus.penrose.session.PenroseSearchResults;
import org.safehaus.penrose.session.PenroseSession;
import org.safehaus.penrose.session.PenroseSearchControls;
import org.safehaus.penrose.Penrose;
import org.safehaus.penrose.engine.Engine;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.pipeline.PipelineAdapter;
import org.safehaus.penrose.pipeline.PipelineEvent;
import org.safehaus.penrose.util.EntryUtil;
import org.safehaus.penrose.util.LDAPUtil;
import org.safehaus.penrose.util.ExceptionUtil;
import org.safehaus.penrose.schema.SchemaManager;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.filter.Filter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.mapping.*;
import org.ietf.ldap.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class SearchHandler {

    Logger log = LoggerFactory.getLogger(getClass());

    private Handler handler;

    public SearchHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     *
     * @param session
     * @param entry
     * @param filter
     * @param sc
     * @param results This will be filled with objects of type Entry.
     * @return return code
     * @throws Exception
     */
    public int search(
            final PenroseSession session,
            final Partition partition,
            final Collection path,
            final Entry entry,
            final Filter filter,
            final PenroseSearchControls sc,
            final PenroseSearchResults results
    ) throws Exception {

        handler.getEngine().getThreadManager().execute(new Runnable() {
            public void run() {

                int rc = LDAPException.SUCCESS;
                try {
                    rc = searchInBackground(session, partition, path, entry, filter, sc, results);

                } catch (LDAPException e) {
                    rc = e.getResultCode();

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    rc = ExceptionUtil.getReturnCode(e);

                } finally {
                    results.setReturnCode(rc);
                    results.close();

                    if (rc == LDAPException.SUCCESS) {
                        log.warn("Search operation succeded.");
                    } else {
                        log.warn("Search operation failed. RC="+rc);
                    }
                }
            }
        });

        return LDAPException.SUCCESS;
    }

    public String normalize(String dn) {
        String newDn = "";

        SchemaManager schemaManager = handler.getSchemaManager();
        while (dn != null) {
            Row rdn = EntryUtil.getRdn(dn);
            String parentDn = EntryUtil.getParentDn(dn);

            Row newRdn = new Row();
            for (Iterator i=rdn.getNames().iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                Object value = rdn.get(name);

                newRdn.set(schemaManager.getNormalizedAttributeName(name), value);
            }

            //log.debug("Normalized rdn "+rdn+" => "+newRdn);

            newDn = EntryUtil.append(newDn, newRdn.toString());
            dn = parentDn;
        }

        return newDn;
    }

    public int searchInBackground(
            PenroseSession session,
            Partition partition,
            Collection path,
            Entry entry,
            final Filter filter,
            final PenroseSearchControls sc,
            final PenroseSearchResults results
    ) throws Exception {

        AttributeValues parentSourceValues = handler.getEngine().getParentSourceValues(partition, path);

        final PenroseSearchResults sr = new PenroseSearchResults();

        sr.addListener(new PipelineAdapter() {
            public void objectAdded(PipelineEvent event) {
                Entry child = (Entry)event.getObject();
                results.add(child);
            }

            public void pipelineClosed(PipelineEvent event) {
                results.setReturnCode(sr.getReturnCode());
            }
        });

        Engine engine = handler.getEngine();

        if (partition.isProxy(entry.getEntryMapping())) {
            engine = handler.getEngine("PROXY");
        }

        engine.search(
                session,
                partition,
                path,
                parentSourceValues,
                entry.getEntryMapping(),
                entry.getDn(),
                filter,
                sc,
                sr
        );

        if (sc.getScope() == LDAPConnection.SCOPE_ONE || sc.getScope() == LDAPConnection.SCOPE_SUB) { // one level or subtree
            log.debug("Searching children of \""+entry.getEntryMapping().getDn()+"\"");

            Collection children = partition.getChildren(entry.getEntryMapping());

            for (Iterator i = children.iterator(); i.hasNext();) {
                EntryMapping childMapping = (EntryMapping) i.next();

                searchChildren(
                        session,
                        partition,
                        path,
                        parentSourceValues,
                        childMapping,
                        entry.getDn(),
                        filter,
                        sc,
                        sr
                );
            }
        }

        sr.close();

        return LDAPException.SUCCESS;
	}

    public void searchChildren(
            PenroseSession session,
            Partition partition,
            Collection parentPath,
            AttributeValues parentSourceValues,
            EntryMapping entryMapping,
            String baseDn,
            Filter filter,
            PenroseSearchControls sc,
            final PenroseSearchResults results) throws Exception {

        log.info("Search child mapping \""+entryMapping.getDn()+"\":");

        final PenroseSearchResults sr = new PenroseSearchResults();

        sr.addListener(new PipelineAdapter() {
            public void objectAdded(PipelineEvent event) {
                Entry child = (Entry)event.getObject();
                results.add(child);
            }
        });

        //PenroseSearchControls newSc = new PenroseSearchControls();
        //newSc.setAttributes(sc.getAttributes());
        //newSc.setScope(sc.getScope());

        //String newBaseDn = baseDn;

        //if (sc.getScope() == LDAPConnection.SCOPE_ONE) {
            //newSc.setScope(LDAPConnection.SCOPE_BASE);
            //newBaseDn = entryMapping.getDn();
        //}

        Engine engine = handler.getEngine();

        if (partition.isProxy(entryMapping)) {
            engine = handler.getEngine("PROXY");
        }

        engine.expand(
                session,
                partition,
                parentPath,
                parentSourceValues,
                entryMapping,
                baseDn,
                filter,
                sc,
                sr
        );

        sr.close();

        //log.debug("Waiting for search results from \""+entryMapping.getDn()+"\".");

        int rc = sr.getReturnCode();
        log.debug("RC: "+rc);

        if (rc != LDAPException.SUCCESS) {
            results.setReturnCode(rc);
            return;
        }

        if (sc.getScope() != LDAPConnection.SCOPE_SUB) return;

        log.debug("Searching children of " + entryMapping.getDn());

        AttributeValues newParentSourceValues = handler.getEngine().shiftParentSourceValues(parentSourceValues);

        Interpreter interpreter = handler.getEngine().getInterpreterManager().newInstance();

        AttributeValues av = handler.getEngine().computeAttributeValues(entryMapping, interpreter);
        if (av != null) {
            for (Iterator j=av.getNames().iterator(); j.hasNext(); ) {
                String name = (String)j.next();
                Collection values = av.get(name);

                newParentSourceValues.add("parent."+name, values);
            }
        }

        interpreter.clear();

        for (Iterator j=newParentSourceValues.getNames().iterator(); j.hasNext(); ) {
            String name = (String)j.next();
            Collection values = newParentSourceValues.get(name);
            log.debug(" - "+name+": "+values);
        }

        Collection newParentPath = new ArrayList();
        newParentPath.add(null);
        newParentPath.addAll(parentPath);

        Collection children = partition.getChildren(entryMapping);

        for (Iterator i = children.iterator(); i.hasNext();) {
            EntryMapping childMapping = (EntryMapping) i.next();

            searchChildren(session, partition, newParentPath, newParentSourceValues, childMapping, baseDn, filter, sc, results);
        }
    }
}
