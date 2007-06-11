package org.safehaus.penrose.adapter.jdbc;

import org.safehaus.penrose.mapping.FieldMapping;
import org.safehaus.penrose.entry.SourceValues;
import org.safehaus.penrose.ldap.RDN;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.ldap.ModRdnRequest;
import org.safehaus.penrose.ldap.ModRdnResponse;
import org.safehaus.penrose.ldap.Attributes;
import org.safehaus.penrose.jdbc.UpdateStatement;
import org.safehaus.penrose.jdbc.UpdateRequest;
import org.safehaus.penrose.jdbc.Assignment;
import org.safehaus.penrose.jdbc.Request;
import org.safehaus.penrose.source.*;
import org.safehaus.penrose.filter.SimpleFilter;
import org.safehaus.penrose.filter.FilterTool;
import org.safehaus.penrose.filter.Filter;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Endi S. Dewata
 */
public class ModRdnRequestBuilder extends RequestBuilder {

    Collection sources;

    SourceValues sourceValues;
    Interpreter interpreter;

    ModRdnRequest request;
    ModRdnResponse response;

    SourceValues newSourceValues = new SourceValues();

    public ModRdnRequestBuilder(
            Collection sources,
            SourceValues sourceValues,
            Interpreter interpreter,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws Exception {

        this.sources = sources;
        this.sourceValues = sourceValues;

        this.interpreter = interpreter;

        this.request = request;
        this.response = response;
    }

    public Collection<Request> generate() throws Exception {

        int sourceCounter = 0;
        for (Iterator i= sources.iterator(); i.hasNext(); sourceCounter++) {
            SourceRef sourceRef = (SourceRef)i.next();

            if (sourceCounter == 0) {
                generatePrimaryRequest(sourceRef);
            } else {
                generateSecondaryRequests(sourceRef);
            }
        }

        return requests;
    }

    public void generatePrimaryRequest(
            SourceRef sourceRef
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        String alias = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+alias);

        UpdateStatement statement = new UpdateStatement();

        Source source = sourceRef.getSource();
        statement.setSource(source);

        interpreter.set(sourceValues);

        RDN newRdn = request.getNewRdn();
        for (String attributeName : newRdn.getNames()) {
            Object attributeValue = newRdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        newSourceValues.set(sourceValues);

        Attributes attributes = newSourceValues.get(alias);

        for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
            Field field = fieldRef.getField();
            String fieldName = field.getName();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            if ("INTEGER".equals(field.getType()) && value instanceof String) {
                value = Integer.parseInt((String)value);
            }

            if (debug) log.debug(" - Field: " + fieldName + ": " + value);
            statement.addAssignment(new Assignment(fieldRef, value));

            attributes.setValue(fieldName, value);
        }

        Filter filter = null;

        attributes = sourceValues.get(alias);
        
        for (String fieldName : attributes.getNames()) {
            Object value = attributes.getValue(fieldName);

            FieldRef fieldRef = sourceRef.getFieldRef(fieldName);

            SimpleFilter sf = new SimpleFilter(fieldName, "=", value);
            filter = FilterTool.appendAndFilter(filter, sf);
        }

        statement.setFilter(filter);

        interpreter.clear();

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        requests.add(updateRequest);
    }

    public void generateSecondaryRequests(
            SourceRef sourceRef
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        String sourceName = sourceRef.getAlias();
        if (debug) log.debug("Processing source "+sourceName);

        UpdateStatement statement = new UpdateStatement();

        Source source = sourceRef.getSource();
        statement.setSource(source);

        interpreter.set(newSourceValues);

        RDN newRdn = request.getNewRdn();
        for (String attributeName : newRdn.getNames()) {
            Object attributeValue = newRdn.get(attributeName);

            interpreter.set(attributeName, attributeValue);
        }

        for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
            Field field = fieldRef.getField();
            String fieldName = field.getName();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            if (debug) log.debug(" - Field: " + fieldName + ": " + value);
            statement.addAssignment(new Assignment(fieldRef, value));
        }

        Filter filter = null;

        for (FieldRef fieldRef : sourceRef.getFieldRefs()) {
            Field field = fieldRef.getField();
            String fieldName = field.getName();

            FieldMapping fieldMapping = fieldRef.getFieldMapping();

            String variable = fieldMapping.getVariable();
            if (variable == null) continue;

            int i = variable.indexOf(".");
            String sn = variable.substring(0, i);
            String fn = variable.substring(i + 1);

            Attributes fields = sourceValues.get(sn);
            if (fields == null) continue;
            
            Object value = fields.getValue(fn);
            if (value == null) continue;

            SimpleFilter sf = new SimpleFilter(fieldName, "=", value);
            filter = FilterTool.appendAndFilter(filter, sf);

            if (debug) log.debug(" - Field: " + fieldName + ": " + value);
        }

        statement.setFilter(filter);

        interpreter.clear();

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.setStatement(statement);

        requests.add(0, updateRequest);
    }
}
