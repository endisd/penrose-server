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
package org.safehaus.penrose.engine.impl;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.graph.GraphVisitor;
import org.safehaus.penrose.graph.GraphIterator;
import org.safehaus.penrose.graph.Graph;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.util.ExceptionUtil;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.connector.Connector;
import org.safehaus.penrose.engine.Engine;
import org.safehaus.penrose.entry.SourceValues;
import org.safehaus.penrose.ldap.RDN;
import org.ietf.ldap.LDAPException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class ModifyGraphVisitor extends GraphVisitor {

    Logger log = LoggerFactory.getLogger(getClass());

    public Engine engine;
    public Partition partition;
    public EntryMapping entryMapping;

    public Graph graph;
    public SourceMapping primarySourceMapping;

    public SourceValues oldSourceValues;
    public SourceValues newSourceValues;
    private SourceValues modifiedSourceValues = new SourceValues();

    public ModifyGraphVisitor(
            Engine engine,
            Partition partition,
            EntryMapping entryMapping,
            SourceValues oldSourceValues,
            SourceValues newSourceValues
            ) throws Exception {

        this.engine = engine;
        this.partition = partition;
        this.entryMapping = entryMapping;

        this.oldSourceValues = oldSourceValues;
        this.newSourceValues = newSourceValues;

        //modifiedSourceValues.add(newSourceValues);

        graph = engine.getGraph(entryMapping);
        primarySourceMapping = engine.getPrimarySource(entryMapping);
    }

    public void run() throws LDAPException {
        try {
            graph.traverse(this, primarySourceMapping);
        } catch (LDAPException e) {
            throw e;

        } catch (Exception e) {
            int rc = ExceptionUtil.getReturnCode(e);
            String message = e.getMessage();
            log.error(message, e);
            throw new LDAPException(LDAPException.resultCodeToString(rc), rc, message);
        }
    }

    public void visitNode(GraphIterator graphIterator, Object node) throws Exception {

        SourceMapping sourceMapping = (SourceMapping)node;

        if (log.isDebugEnabled()) {
            log.debug(Formatter.displaySeparator(40));
            log.debug(Formatter.displayLine("Visiting "+sourceMapping.getName(), 40));
            log.debug(Formatter.displaySeparator(40));
        }

        //if (sourceMapping.isReadOnly() || !sourceMapping.isIncludeOnModify()) {
        //    log.debug("Source "+sourceMapping.getName()+" is not included on modify");
        //    graphIterator.traverseEdges(node);
        //    return;
        //}

        if (entryMapping.getSourceMapping(sourceMapping.getName()) == null) {
            log.debug("Source "+sourceMapping.getName()+" is not defined in entry "+entryMapping.getDn());
            graphIterator.traverseEdges(node);
            return;
        }

        log.debug("Old values:");
        SourceValues oldValues = new SourceValues();
        for (Iterator i=oldSourceValues.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (!name.startsWith(sourceMapping.getName()+".")) continue;

            //Collection values = oldSourceValues.get(name);
            //log.debug(" - "+name+": "+values);

            name = name.substring(sourceMapping.getName().length()+1);
            //oldValues.set(name, values);
        }

        log.debug("New values:");
        SourceValues newValues = new SourceValues();
        for (Iterator i=newSourceValues.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (!name.startsWith(sourceMapping.getName()+".")) continue;

            //Collection values = newSourceValues.get(name);
            //log.debug(" - "+name+": "+values);

            name = name.substring(sourceMapping.getName().length()+1);
            //newValues.set(name, values);
        }

        SourceConfig sourceConfig = partition.getSources().getSourceConfig(sourceMapping.getSourceName());
        Connector connector = engine.getConnector(sourceConfig);

        RDN pk = new RDN();
        Collection modifications = new ArrayList();
/*
        connector.modify(
                partition,
                sourceConfig,
                pk,
                modifications,
                oldValues,
                newValues,
                null,
                null
        );
*/
        modifiedSourceValues.remove(sourceMapping.getName());
        //modifiedSourceValues.set(sourceMapping.getName(), newValues);

        graphIterator.traverseEdges(node);
    }

    public SourceValues getModifiedSourceValues() {
        return modifiedSourceValues;
    }

    public void setModifiedSourceValues(SourceValues modifiedSourceValues) {
        this.modifiedSourceValues = modifiedSourceValues;
    }
}