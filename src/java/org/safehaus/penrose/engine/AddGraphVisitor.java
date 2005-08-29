/**
 * Copyright (c) 1998-2005, Verge Lab., LLC.
 * All rights reserved.
 */
package org.safehaus.penrose.engine;

import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.sync.SyncService;
import org.safehaus.penrose.graph.GraphVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ietf.ldap.LDAPException;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class AddGraphVisitor extends GraphVisitor {

    Logger log = LoggerFactory.getLogger(getClass());

    public EngineContext engineContext;
    public EntryDefinition entryDefinition;
    public AttributeValues values;
    private int returnCode = LDAPException.SUCCESS;

    private Stack stack = new Stack();

    public AddGraphVisitor(
            EngineContext engineContext,
            Source primarySource,
            EntryDefinition entryDefinition,
            AttributeValues values
            ) throws Exception {

        this.engineContext = engineContext;
        this.entryDefinition = entryDefinition;
        this.values = values;

        Collection rows = engineContext.getTransformEngine().convert(values);
        Collection keys = new HashSet();
/*
        for (Iterator i=rows.iterator(); i.hasNext(); ) {
            Row row = (Row)i.next();
            log.debug(" - "+row);

            Interpreter interpreter = engine.getEngineContext().newInterpreter();
            interpreter.set(row);

            Collection fields = primarySource.getPrimaryKeyFields();

            Row pk = new Row();
            boolean valid = true;

            for (Iterator j=fields.iterator(); j.hasNext(); ) {
                Field field = (Field)j.next();
                String name = field.getName();
                String expression = field.getExpression();
                Object value = interpreter.eval(expression);

                if (value == null) {
                    valid = false;
                    break;
                }

                pk.set(name, value);
            }

            if (!valid) continue;

            keys.add(pk);
        }
*/
        log.debug("Primary keys: "+keys);
        stack.push(keys);
    }

    public boolean preVisitNode(Object node, Object parameter) throws Exception {
        Source source = (Source)node;
        //log.debug("Source "+source.getName());

        if (entryDefinition.getSource(source.getName()) == null) return false;

        returnCode = engineContext.getSyncService().add(source, entryDefinition, values);

        if (returnCode == LDAPException.NO_SUCH_OBJECT) return true; // ignore
        if (returnCode != LDAPException.SUCCESS) return false;

        return true;
    }

    public boolean preVisitEdge(Object node1, Object node2, Object edge, Object parameter) throws Exception {
        Source source = (Source)node2;
        Relationship relationship = (Relationship)edge;

        log.debug("Relationship "+relationship);
        if (entryDefinition.getSource(source.getName()) == null) return false;

        String lhs = relationship.getLhs();
        String rhs = relationship.getRhs();

        if (lhs.startsWith(source.getName()+".")) {
            String exp = lhs;
            lhs = rhs;
            rhs = exp;
        }

        int li = lhs.indexOf(".");
        String lField = lhs.substring(li+1);

        int ri = rhs.indexOf(".");
        String rField = rhs.substring(ri+1);

        Collection rows = (Collection)stack.peek();
        //log.debug("Rows: "+rows);

        Collection newRows = new HashSet();
        for (Iterator i=rows.iterator(); i.hasNext(); ) {
            Row row = (Row)i.next();

            Object value = row.get(lField);
            Row newRow = new Row();
            newRow.set(rField, value);

            newRows.add(newRow);

            //log.debug(lField+" = "+rField+" => "+value);
        }
        //log.debug("New Rows: "+newRows);

        stack.push(newRows);

        return true;
    }

    public void postVisitEdge(Object node1, Object node2, Object edge, Object parameter) throws Exception {
        stack.pop();
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
