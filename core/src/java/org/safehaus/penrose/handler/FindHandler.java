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

import org.safehaus.penrose.session.PenroseSession;
import org.safehaus.penrose.util.EntryUtil;
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.mapping.*;
import org.safehaus.penrose.engine.Engine;
import org.ietf.ldap.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class FindHandler {

    Logger log = LoggerFactory.getLogger(getClass());

    private Handler handler;

    public FindHandler(Handler handler) {
        this.handler = handler;
    }

    /**
	 * Find an entry given a dn.
	 *
	 * @param dn
	 * @return path from the entry to the root entry
	 */
    public Entry find(
            PenroseSession session,
            String dn) throws Exception {

        Partition partition = handler.getPartitionManager().findPartition(dn);
        if (partition == null) return null;

        Collection path = find(session, partition, dn);
        if (path.size() == 0) return null;

        return (Entry)path.iterator().next();
    }

    /**
     * @return path (List of Entries).
     */
    public Collection find(
            PenroseSession session,
            Partition partition,
            String dn) throws Exception {

        List path = new ArrayList();
        String entryDn = null;

        while (dn != null) {
            String suffix = EntryUtil.getSuffix(dn);
            entryDn = EntryUtil.append(suffix, entryDn);

            Entry entry = find(session, partition, path, entryDn);
            path.add(0, entry);

            dn = EntryUtil.getPrefix(dn);
        }

        return path;
    }

    public Entry find(
            PenroseSession session,
            Partition partition,
            Collection parentPath,
            String dn) throws Exception {

        log.debug("Finding entry \""+dn+"\".");

        if (dn == null) return null;
        if (partition == null) return null;

        AttributeValues parentSourceValues = handler.getEngine().getParentSourceValues(partition, parentPath);

        Collection entryMappings = partition.findEntryMappings(dn);
        if (entryMappings == null) return null;

        for (Iterator iterator = entryMappings.iterator(); iterator.hasNext(); ) {
            EntryMapping entryMapping = (EntryMapping) iterator.next();

            int rc = handler.getACLEngine().checkSearch(session, dn, entryMapping);
            if (rc != LDAPException.SUCCESS) {
                log.debug("Checking search permission => FAILED");
                throw new LDAPException("Insufficient access rights", LDAPException.INSUFFICIENT_ACCESS_RIGHTS, "Insufficient access rights");
            }

            Engine engine = handler.getEngine();

            if (partition.isProxy(entryMapping)) {
                engine = handler.getEngine("PROXY");
            }

            Entry entry = engine.find(session, partition, parentPath, parentSourceValues, entryMapping, dn);
            if (entry != null) return entry;
        }

        log.debug("Can't find \""+dn+"\".");

        return null;
	}

}