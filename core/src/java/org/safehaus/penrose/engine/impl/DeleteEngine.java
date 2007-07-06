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
import org.safehaus.penrose.partition.Partition;
import org.safehaus.penrose.util.ExceptionUtil;
import org.safehaus.penrose.entry.Entry;
import org.safehaus.penrose.entry.SourceValues;
import org.ietf.ldap.LDAPException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Endi S. Dewata
 */
public class DeleteEngine {

    Logger log = LoggerFactory.getLogger(getClass());

    EngineImpl engine;

    public DeleteEngine(EngineImpl engine) {
        this.engine = engine;
    }

    public void delete(Partition partition, Entry entry) throws LDAPException {

        try {
            EntryMapping entryMapping = null; // entry.getEntryMapping();

            SourceValues sourceValues = null; // entry.getSourceValues();
            //getFieldValues(entry.getDn(), sourceValues);

            log.debug("Deleting entry "+entry.getDn()+" ["+sourceValues+"]");

            DeleteGraphVisitor visitor = new DeleteGraphVisitor(engine, partition, entryMapping, sourceValues);
            visitor.run();

        } catch (LDAPException e) {
            throw e;

        } catch (Exception e) {
            int rc = ExceptionUtil.getReturnCode(e);
            String message = e.getMessage();
            log.error(message, e);
            throw new LDAPException(LDAPException.resultCodeToString(rc), rc, message);
        }
    }
}