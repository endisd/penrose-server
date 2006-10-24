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
package org.safehaus.penrose.engine;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.graph.GraphVisitor;
import org.safehaus.penrose.graph.GraphIterator;
import org.safehaus.penrose.graph.Graph;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.SourceConfig;
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

    public AttributeValues oldSourceValues;
    public AttributeValues newSourceValues;
    private AttributeValues modifiedSourceValues = new AttributeValues();

    private int returnCode = LDAPException.SUCCESS;

    public ModifyGraphVisitor(
            Engine engine,
            Partition partition,
            EntryMapping entryMapping,
            AttributeValues oldSourceValues,
            AttributeValues newSourceValues
            ) throws Exception {

        this.engine = engine;
        this.partition = partition;
        this.entryMapping = entryMapping;

        this.oldSourceValues = oldSourceValues;
        this.newSourceValues = newSourceValues;

        modifiedSourceValues.add(newSourceValues);

        graph = engine.getPartitionManager().getGraph(partition, entryMapping);
        primarySourceMapping = engine.getPartitionManager().getPrimarySource(partition, entryMapping);
    }

    public void run() throws Exception {
        graph.traverse(this, primarySourceMapping);
    }

    public void visitNode(GraphIterator graphIterator, Object node) throws Exception {

        SourceMapping sourceMapping = (SourceMapping)node;

        if (log.isDebugEnabled()) {
            log.debug(Formatter.displaySeparator(40));
            log.debug(Formatter.displayLine("Visiting "+sourceMapping.getName(), 40));
            log.debug(Formatter.displaySeparator(40));
        }

        if (sourceMapping.isReadOnly() || !sourceMapping.isIncludeOnModify()) {
            log.debug("Source "+sourceMapping.getName()+" is not included on modify");
            graphIterator.traverseEdges(node);
            return;
        }

        if (entryMapping.getSourceMapping(sourceMapping.getName()) == null) {
            log.debug("Source "+sourceMapping.getName()+" is not defined in entry "+entryMapping.getDn());
            graphIterator.traverseEdges(node);
            return;
        }

        log.debug("Old values:");
        AttributeValues oldValues = new AttributeValues();
        for (Iterator i=oldSourceValues.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (!name.startsWith(sourceMapping.getName()+".")) continue;

            Collection values = oldSourceValues.get(name);
            log.debug(" - "+name+": "+values);

            name = name.substring(sourceMapping.getName().length()+1);
            oldValues.set(name, values);
        }

        log.debug("New values:");
        AttributeValues newValues = new AttributeValues();
        for (Iterator i=newSourceValues.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (!name.startsWith(sourceMapping.getName()+".")) continue;

            Collection values = newSourceValues.get(name);
            log.debug(" - "+name+": "+values);

            name = name.substring(sourceMapping.getName().length()+1);
            newValues.set(name, values);
        }

        SourceConfig sourceConfig = partition.getSourceConfig(sourceMapping.getSourceName());

        returnCode = engine.getConnector(sourceConfig).modify(partition, sourceConfig, oldValues, newValues);
        if (returnCode != LDAPException.SUCCESS) return;

        modifiedSourceValues.remove(sourceMapping.getName());
        modifiedSourceValues.set(sourceMapping.getName(), newValues);

        graphIterator.traverseEdges(node);
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public AttributeValues getModifiedSourceValues() {
        return modifiedSourceValues;
    }

    public void setModifiedSourceValues(AttributeValues modifiedSourceValues) {
        this.modifiedSourceValues = modifiedSourceValues;
    }
}
