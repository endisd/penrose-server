package org.safehaus.penrose.engine.basic;

import org.safehaus.penrose.engine.Engine;
import org.safehaus.penrose.engine.EngineTool;
import org.safehaus.penrose.session.*;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.partition.SourceConfig;
import org.safehaus.penrose.partition.FieldConfig;
import org.safehaus.penrose.mapping.EntryMapping;
import org.safehaus.penrose.mapping.SourceMapping;
import org.safehaus.penrose.mapping.FieldMapping;
import org.safehaus.penrose.mapping.AttributeMapping;
import org.safehaus.penrose.entry.*;
import org.safehaus.penrose.connector.Connector;
import org.safehaus.penrose.util.Formatter;
import org.safehaus.penrose.util.LDAPUtil;
import org.safehaus.penrose.util.ExceptionUtil;
import org.safehaus.penrose.interpreter.Interpreter;
import org.safehaus.penrose.source.SourceRef;
import org.safehaus.penrose.source.Sources;
import org.safehaus.penrose.ldap.*;
import org.ietf.ldap.LDAPException;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class BasicEngine extends Engine {

    public final static String  FETCH         = "fetch";
    public final static boolean DEFAULT_FETCH = false;

    BasicSearchEngine searchEngine;

    boolean fetch = DEFAULT_FETCH;

    public void init() throws Exception {
        super.init();

        searchEngine = new BasicSearchEngine(this);

        String s = getParameter(FETCH);
        if (s != null) fetch = Boolean.valueOf(s);

        log.debug("Default engine initialized.");
    }

    public BasicSearchEngine getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(BasicSearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public void start() throws Exception {
        super.start();

        //log.debug("Starting Engine...");

        for (Partition partition : partitionManager.getPartitions()) {

            for (EntryMapping entryMapping : partition.getRootEntryMappings()) {
                analyzer.analyze(partition, entryMapping);
            }
        }

        //threadManager.execute(new RefreshThread(this));

        //log.debug("Engine started.");
    }

    public void stop() throws Exception {
        if (stopping) return;

        log.debug("Stopping Engine...");
        stopping = true;

        // wait for all the worker threads to finish
        //if (threadManager != null) threadManager.stopRequestAllWorkers();
        log.debug("Engine stopped.");
        super.stop();
    }

    public void extractSourceValues(
            Partition partition,
            EntryMapping entryMapping,
            DN dn,
            SourceValues sourceValues
    ) throws Exception {

        Interpreter interpreter = interpreterManager.newInstance();

        boolean debug = log.isDebugEnabled();
        if (debug) log.debug("Extracting source values from "+dn);

        extractSourceValues(
                partition,
                interpreter,
                dn,
                entryMapping,
                sourceValues
        );
    }

    public void extractSourceValues(
            Partition partition,
            Interpreter interpreter,
            DN dn,
            EntryMapping entryMapping,
            SourceValues sourceValues
    ) throws Exception {

        DN parentDn = dn.getParentDn();
        EntryMapping em = partition.getParent(entryMapping);

        if (parentDn != null && em != null) {
            extractSourceValues(partition, interpreter, parentDn, em, sourceValues);
        }

        RDN rdn = dn.getRdn();
        Collection<SourceMapping> sourceMappings = entryMapping.getSourceMappings();

        //if (sourceMappings.isEmpty()) return;
        //SourceMapping sourceMapping = sourceMappings.iterator().next();

        //interpreter.set(sourceValues);
        interpreter.set(rdn);

        for (SourceMapping sourceMapping : sourceMappings) {
            extractSourceValues(
                    partition,
                    interpreter,
                    rdn,
                    entryMapping,
                    sourceMapping,
                    sourceValues
            );
        }

        interpreter.clear();
    }

    public void extractSourceValues(
            Partition partition,
            Interpreter interpreter,
            RDN rdn,
            EntryMapping entryMapping,
            SourceMapping sourceMapping,
            SourceValues sourceValues
    ) throws Exception {

        boolean debug = log.isDebugEnabled();
        if (debug) log.debug("Extracting source "+sourceMapping.getName()+" from RDN: "+rdn);

        Attributes attributes = sourceValues.get(sourceMapping.getName());

        Sources sources = partition.getSources();
        SourceConfig sourceConfig = sources.getSourceConfig(sourceMapping.getSourceName());

        Collection<FieldMapping> fieldMappings = sourceMapping.getFieldMappings();
        for (FieldMapping fieldMapping : fieldMappings) {
            FieldConfig fieldConfig = sourceConfig.getFieldConfig(fieldMapping.getName());

            Object value = interpreter.eval(fieldMapping);
            if (value == null) continue;

            if ("INTEGER".equals(fieldConfig.getType()) && value instanceof String) {
                value = Integer.parseInt((String)value);
            }

            attributes.addValue(fieldMapping.getName(), value);

            String fieldName = sourceMapping.getName() + "." + fieldMapping.getName();
            if (debug) log.debug(" => " + fieldName + ": " + value);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Add
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(
            Session session,
            Partition partition,
            EntryMapping entryMapping,
            AddRequest request,
            AddResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        DN dn = request.getDn();

        SourceValues sourceValues = new SourceValues();

        boolean fetchEntry = fetch;

        String s = entryMapping.getParameter(FETCH);
        if (s != null) fetchEntry = Boolean.valueOf(s);

        if (fetchEntry) {
            EntryMapping parentMapping = partition.getParent(entryMapping);
            DN parentDn = dn.getParentDn();

            SearchResult parent = find(session, partition, parentMapping, parentDn);
            sourceValues.add(parent.getSourceValues());

            EngineTool.propagateDown(partition, entryMapping, sourceValues);

        } else {
            extractSourceValues(partition, entryMapping, dn, sourceValues);
        }

        if (debug) {
            log.debug("Source values:");
            sourceValues.print();
        }

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("ADD", 80));
            log.debug(Formatter.displayLine("DN            : "+dn, 80));
            log.debug(Formatter.displayLine("Entry Mapping : "+entryMapping.getDn(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        Collection<Collection<SourceRef>> groupsOfSources = createGroupsOfSources(partition, entryMapping);

        Iterator<Collection<SourceRef>> iterator = groupsOfSources.iterator();

        Collection<SourceRef> sourceRefs = iterator.next();
        Connector connector = getConnector(sourceRefs.iterator().next());

        Collection<SourceRef> localSourceRefs = new ArrayList<SourceRef>();

        for (SourceRef sourceRef : sourceRefs) {
            if (entryMapping.getSourceMapping(sourceRef.getAlias()) != null) {
                localSourceRefs.add(sourceRef);
            }
        }

        connector.add(
                session,
                partition,
                entryMapping,
                localSourceRefs,
                sourceValues,
                request,
                response
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Bind
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void bind(
            Session session,
            Partition partition,
            EntryMapping entryMapping,
            BindRequest request,
            BindResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        DN dn = request.getDn();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("BIND", 80));
            log.debug(Formatter.displayLine("DN            : "+dn, 80));
            log.debug(Formatter.displayLine("Entry Mapping : "+entryMapping.getDn(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        SourceValues sourceValues = new SourceValues();

        boolean fetchEntry = fetch;

        String s = entryMapping.getParameter(FETCH);
        if (s != null) fetchEntry = Boolean.valueOf(s);

        if (fetchEntry) {
            SearchResult entry = find(session, partition, entryMapping, dn);
            sourceValues.add(entry.getSourceValues());
        } else {
            extractSourceValues(partition, entryMapping, dn, sourceValues);
            EngineTool.propagateDown(partition, entryMapping, sourceValues);
        }

        if (debug) {
            log.debug("Source values:");
            sourceValues.print();
        }

        Collection<Collection<SourceRef>> groupsOfSources = createGroupsOfSources(partition, entryMapping);

        boolean success = true;
        boolean found = false;

        for (Collection<SourceRef> sourceRefs : groupsOfSources) {

            SourceRef sourceRef = sourceRefs.iterator().next();
            Connector connector = getConnector(sourceRef);

            String flag = sourceRef.getBind();
            if (debug) log.debug("Flag: "+flag);
            
            if (SourceMapping.IGNORE.equals(flag)) {
                continue;
            }

            found = true;

            try {
                connector.bind(
                        session,
                        partition,
                        entryMapping,
                        sourceRefs,
                        sourceValues,
                        request,
                        response
                );

                if (flag == null || SourceMapping.SUFFICIENT.equals(flag)) {
                    if (debug) log.debug("Bind is sufficient.");
                    return;
                }

            } catch (Exception e) {

                if (debug) log.debug(e.getMessage());

                if (SourceMapping.REQUISITE.equals(flag)) {
                    if (debug) log.debug("Bind is requisite.");
                    log.error(e.getMessage(), e);
                    throw e;
                    
                } else {
                    success = false;
                }
            }
        }

        if (found) {
            if (!success) {
                if (debug) log.debug("No successful bind.");
                throw ExceptionUtil.createLDAPException(LDAPException.INVALID_CREDENTIALS);
            }
            
        } else {
            log.debug("Calling default bind operation.");
            super.bind(session, partition, entryMapping, request, response);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Delete
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void delete(
            Session session,
            Partition partition,
            EntryMapping entryMapping,
            DeleteRequest request,
            DeleteResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        DN dn = request.getDn();

        SourceValues sourceValues = new SourceValues();

        boolean fetchEntry = fetch;

        String s = entryMapping.getParameter(FETCH);
        if (s != null) fetchEntry = Boolean.valueOf(s);

        if (fetchEntry) {
            SearchResult entry = find(session, partition, entryMapping, dn);
            sourceValues.add(entry.getSourceValues());
        } else {
            extractSourceValues(partition, entryMapping, dn, sourceValues);
        }

        if (debug) {
            log.debug("Source values:");
            sourceValues.print();
        }

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("DELETE", 80));
            log.debug(Formatter.displayLine("DN            : "+dn, 80));
            log.debug(Formatter.displayLine("Entry Mapping : "+entryMapping.getDn(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        Collection<Collection<SourceRef>> groupsOfSources = createGroupsOfSources(partition, entryMapping);

        Iterator<Collection<SourceRef>> iterator = groupsOfSources.iterator();

        Collection<SourceRef> sourceRefs = iterator.next();
        Connector connector = getConnector(sourceRefs.iterator().next());

        Collection<SourceRef> localSourceRefs = new ArrayList<SourceRef>();

        for (SourceRef sourceRef : sourceRefs) {
            if (entryMapping.getSourceMapping(sourceRef.getAlias()) != null) {
                localSourceRefs.add(sourceRef);
            }
        }

        connector.delete(
                session,
                partition,
                entryMapping,
                localSourceRefs,
                sourceValues,
                request,
                response
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modify
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modify(
            Session session,
            Partition partition,
            EntryMapping entryMapping,
            ModifyRequest request,
            ModifyResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        DN dn = request.getDn();

        SourceValues sourceValues = new SourceValues();

        boolean fetchEntry = fetch;

        String s = entryMapping.getParameter(FETCH);
        if (s != null) fetchEntry = Boolean.valueOf(s);

        if (fetchEntry) {
            SearchResult entry = find(session, partition, entryMapping, dn);
            sourceValues.add(entry.getSourceValues());
        } else {
            extractSourceValues(partition, entryMapping, dn, sourceValues);
        }

        if (debug) {
            log.debug("Source values:");
            sourceValues.print();
        }

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("MODIFY", 80));
            log.debug(Formatter.displayLine("DN            : "+dn, 80));
            log.debug(Formatter.displayLine("Entry Mapping : "+entryMapping.getDn(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        Collection<Collection<SourceRef>> groupsOfSources = createGroupsOfSources(partition, entryMapping);

        Iterator<Collection<SourceRef>> iterator = groupsOfSources.iterator();

        Collection<SourceRef> sourceRefs = iterator.next();
        Connector connector = getConnector(sourceRefs.iterator().next());

        Collection<SourceRef> localSourceRefs = new ArrayList<SourceRef>();

        for (SourceRef sourceRef : sourceRefs) {
            if (entryMapping.getSourceMapping(sourceRef.getAlias()) != null) {
                localSourceRefs.add(sourceRef);
            }
        }

        connector.modify(
                session,
                partition,
                entryMapping,
                localSourceRefs,
                sourceValues,
                request,
                response
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ModRDN
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void modrdn(
            Session session,
            Partition partition,
            EntryMapping entryMapping,
            ModRdnRequest request,
            ModRdnResponse response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        DN dn = request.getDn();

        SourceValues sourceValues = new SourceValues();

        boolean fetchEntry = fetch;

        String s = entryMapping.getParameter(FETCH);
        if (s != null) fetchEntry = Boolean.valueOf(s);

        if (fetchEntry) {
            SearchResult entry = find(session, partition, entryMapping, dn);
            sourceValues.add(entry.getSourceValues());
        } else {
            extractSourceValues(partition, entryMapping, dn, sourceValues);
        }

        if (debug) {
            log.debug("Source values:");
            sourceValues.print();
        }

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("MODRDN", 80));
            log.debug(Formatter.displayLine("DN            : "+dn, 80));
            log.debug(Formatter.displayLine("Entry Mapping : "+entryMapping.getDn(), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        Collection<Collection<SourceRef>> groupsOfSources = createGroupsOfSources(partition, entryMapping);

        Iterator<Collection<SourceRef>> iterator = groupsOfSources.iterator();

        Collection<SourceRef> sourceRefs = iterator.next();
        Connector connector = getConnector(sourceRefs.iterator().next());

        Collection<SourceRef> localSourceRefs = new ArrayList<SourceRef>();

        for (SourceRef sourceRef : sourceRefs) {
            if (entryMapping.getSourceMapping(sourceRef.getAlias()) != null) {
                localSourceRefs.add(sourceRef);
            }
        }

        connector.modrdn(
                session,
                partition,
                entryMapping,
                localSourceRefs,
                sourceValues,
                request,
                response
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Search
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void search(
            Session session,
            Partition partition,
            EntryMapping baseMapping,
            EntryMapping entryMapping,
            SearchRequest request,
            SearchResponse<SearchResult> response
    ) throws Exception {

        boolean debug = log.isDebugEnabled();

        DN dn = request.getDn();

        if (debug) {
            log.debug(Formatter.displaySeparator(80));
            log.debug(Formatter.displayLine("SEARCH", 80));
            log.debug(Formatter.displayLine("Base DN       : "+dn, 80));
            log.debug(Formatter.displayLine("Base Mapping  : "+baseMapping.getDn(), 80));
            log.debug(Formatter.displayLine("Entry Mapping : "+entryMapping.getDn(), 80));
            log.debug(Formatter.displayLine("Filter        : "+request.getFilter(), 80));
            log.debug(Formatter.displayLine("Scope         : "+LDAPUtil.getScope(request.getScope()), 80));
            log.debug(Formatter.displaySeparator(80));
        }

        try {
            SourceValues sourceValues = new SourceValues();

            //if (fetch) {
            //    SearchResult entry = find(session, partition, baseMapping, dn);
            //    sourceValues.add(entry.getSourceValues());
            //} else {
                extractSourceValues(partition, baseMapping, dn, sourceValues);
            //}
            //EngineTool.propagateDown(partition, entryMapping, sourceValues);

            if (debug) {
                log.debug("Source values:");
                sourceValues.print();
            }

            searchEngine.search(
                    session,
                    partition,
                    baseMapping,
                    entryMapping,
                    sourceValues,
                    request,
                    response
            );

        } finally {
            response.close();
        }
    }

    public Attributes computeAttributes(
            Interpreter interpreter,
            EntryMapping entryMapping
    ) throws Exception {

        Attributes attributes = new Attributes();

        Collection<AttributeMapping> attributeMappings = entryMapping.getAttributeMappings();

        for (AttributeMapping attributeMapping : attributeMappings) {

            Object value = interpreter.eval(attributeMapping);
            if (value == null) continue;

            if (value instanceof Collection) {
                attributes.addValues(attributeMapping.getName(), (Collection) value);
            } else {
                attributes.addValue(attributeMapping.getName(), value);
            }
        }

        Collection<String> objectClasses = entryMapping.getObjectClasses();
        for (String objectClass : objectClasses) {
            attributes.addValue("objectClass", objectClass);
        }

        return attributes;
    }
}
