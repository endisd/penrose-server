package org.safehaus.penrose.connector;

import org.safehaus.penrose.connector.Adapter;
import org.safehaus.penrose.session.PenroseSearchResults;
import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.filter.Filter;

import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public class Connection {

    private ConnectionConfig connectionConfig;
    private Adapter adapter;

    public void init(ConnectionConfig connectionConfig, AdapterConfig adapterConfig) throws Exception {
        this.connectionConfig = connectionConfig;

        String adapterClass = adapterConfig.getAdapterClass();
        Class clazz = Class.forName(adapterClass);
        adapter = (Adapter)clazz.newInstance();
        
        adapter.init(adapterConfig, this);
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public String getParameter(String name) {
        return connectionConfig.getParameter(name);
    }

    public Collection getParameterNames() {
        return connectionConfig.getParameterNames();
    }

    public String getConnectionName() {
        return connectionConfig.getConnectionName();
    }

    public int bind(SourceDefinition sourceDefinition, AttributeValues values, String password) throws Exception {
        return adapter.bind(sourceDefinition, values, password);
    }

    public PenroseSearchResults search(SourceDefinition sourceDefinition, Filter filter, long sizeLimit) throws Exception {
        return adapter.search(sourceDefinition, filter, sizeLimit);
    }

    public PenroseSearchResults load(SourceDefinition sourceDefinition, Filter filter, long sizeLimit) throws Exception {
        return adapter.load(sourceDefinition, filter, sizeLimit);
    }

    public int add(SourceDefinition sourceDefinition, AttributeValues values) throws Exception {
        return adapter.add(sourceDefinition, values);
    }

    public int modify(SourceDefinition sourceDefinition, AttributeValues oldValues, AttributeValues newValues) throws Exception {
        return adapter.modify(sourceDefinition, oldValues, newValues);
    }

    public int delete(SourceDefinition sourceDefinition, AttributeValues values) throws Exception {
        return adapter.delete(sourceDefinition, values);
    }

    public int getLastChangeNumber(SourceDefinition sourceDefinition) throws Exception {
        return adapter.getLastChangeNumber(sourceDefinition);
    }

    public PenroseSearchResults getChanges(SourceDefinition sourceDefinition, int lastChangeNumber) throws Exception {
        return adapter.getChanges(sourceDefinition, lastChangeNumber);
    }
}
