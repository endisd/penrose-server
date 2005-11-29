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
package org.safehaus.penrose.engine;

import org.apache.log4j.Logger;
import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.interpreter.Interpreter;
import org.ietf.ldap.LDAPException;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Endi S. Dewata
 */
public class ModRdnEngine {

    Logger log = Logger.getLogger(getClass());

    Engine engine;

    public ModRdnEngine(Engine engine) {
        this.engine = engine;
    }

    public int modrdn(Entry entry, String newRdn) throws Exception {

        EntryMapping entryMapping = entry.getEntryMapping();
        AttributeValues oldAttributeValues = entry.getAttributeValues();
        AttributeValues oldSourceValues = entry.getSourceValues();

        Row rdn = Entry.getRdn(newRdn);

        AttributeValues newAttributeValues = new AttributeValues(oldAttributeValues);
        Collection rdnAttributes = entryMapping.getRdnAttributes();
        for (Iterator i=rdnAttributes.iterator(); i.hasNext(); ) {
            AttributeMapping attributeMapping = (AttributeMapping)i.next();
            String name = attributeMapping.getName();
            String newValue = (String)rdn.get(name);

            newAttributeValues.remove(name);
            newAttributeValues.add(name, newValue);
        }

        AttributeValues newSourceValues = new AttributeValues(oldSourceValues);
        Collection sources = entryMapping.getSourceMappings();
        for (Iterator i=sources.iterator(); i.hasNext(); ) {
            SourceMapping sourceMapping = (SourceMapping)i.next();

            AttributeValues output = new AttributeValues();
            engine.getTransformEngine().translate(sourceMapping, newAttributeValues, output);
            newSourceValues.set(sourceMapping.getName(), output);
        }

        if (log.isDebugEnabled()) {
            log.debug("Attribute values:");
            for (Iterator iterator = newAttributeValues.getNames().iterator(); iterator.hasNext(); ) {
                String name = (String)iterator.next();
                Collection values = newAttributeValues.get(name);
                log.debug(" - "+name+": "+values);
            }

            log.debug("Old source values:");
            for (Iterator iterator = oldSourceValues.getNames().iterator(); iterator.hasNext(); ) {
                String name = (String)iterator.next();
                Collection values = oldSourceValues.get(name);
                log.debug(" - "+name+": "+values);
            }

            log.debug("New source values:");
            for (Iterator iterator = newSourceValues.getNames().iterator(); iterator.hasNext(); ) {
                String name = (String)iterator.next();
                Collection values = newSourceValues.get(name);
                log.debug(" - "+name+": "+values);
            }
        }

        ModRdnGraphVisitor visitor = new ModRdnGraphVisitor(engine, entryMapping, oldSourceValues, newSourceValues);
        visitor.run();

        if (visitor.getReturnCode() != LDAPException.SUCCESS) return visitor.getReturnCode();

        engine.getCache(entry.getParentDn(), entryMapping).remove(entry.getRdn());

        Interpreter interpreter = engine.getInterpreterFactory().newInstance();

        AttributeValues sourceValues = visitor.getModifiedSourceValues();
        AttributeValues attributeValues = engine.computeAttributeValues(entryMapping, sourceValues, interpreter);
        Row newRdn2 = entryMapping.getRdn(attributeValues);
        String dn = newRdn2+","+entry.getParentDn();

        Entry newEntry = new Entry(dn, entryMapping, sourceValues, attributeValues);

        engine.getCache(entry.getParentDn(), entryMapping).put(newRdn2, newEntry);

        return LDAPException.SUCCESS;
    }
}