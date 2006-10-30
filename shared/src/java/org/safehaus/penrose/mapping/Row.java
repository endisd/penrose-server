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
package org.safehaus.penrose.mapping;

import org.ietf.ldap.LDAPDN;

import java.util.TreeMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class holds source's column values. Each value is an single object, not necessarily a collection.
 *
 * @author Endi S. Dewata
 */
public class Row implements Comparable {

    public Map values = new TreeMap();

    public Row() {
    }

    public Row(Map values) {
        this.values.putAll(values);
    }

    public Row(Row row) {
        values.putAll(row.getValues());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    public void add(Row row) {
        values.putAll(row.getValues());
    }

    public void set(Row row) {
        values.clear();
        values.putAll(row.getValues());
    }

    public void add(String prefix, Row row) {
        for (Iterator i=row.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = row.get(name);
            values.put(prefix == null ? name : prefix+"."+name, value);
        }
    }

    public void set(String prefix, Row row) {
        values.clear();
        for (Iterator i=row.getNames().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = row.get(name);
            values.put(prefix == null ? name : prefix+"."+name, value);
        }
    }

    public void set(String name, Object value) {
        this.values.put(name, value);
    }

    public Object get(String name) {
        return values.get(name);
    }

    public Object remove(String name) {
        return values.remove(name);
    }
    
    public Collection getNames() {
        return values.keySet();
    }

    public boolean contains(String name) {
        return values.containsKey(name);
    }

    public Map getValues() {
        return values;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=values.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = values.get(name);
            String rdn = name+"="+value;

            if (sb.length() > 0) sb.append("+");
            sb.append(LDAPDN.escapeRDN(rdn));
        }

        return sb.toString();
    }

    public int hashCode() {
        //System.out.println("Row "+values+" hash code: "+values.hashCode());
        return values.hashCode();
    }

    public boolean equals(Object object) {
        //System.out.println("Comparing row "+values+" with "+object);
        if (object == null) return false;
        if (!(object instanceof Row)) return false;
        Row row = (Row)object;
        return values.equals(row.values);
    }

    public int compareTo(Object object) {

        int c = 0;

        try {
            if (object == null) return 0;
            if (!(object instanceof Row)) return 0;

            Row row = (Row)object;

            Iterator i = values.keySet().iterator();
            Iterator j = row.values.keySet().iterator();

            while (i.hasNext() && j.hasNext()) {
                String name1 = (String)i.next();
                String name2 = (String)j.next();

                c = name1.compareTo(name2);
                if (c != 0) return c;

                Object value1 = values.get(name1);
                Object value2 = row.values.get(name2);

                if (value1 instanceof Comparable && value2 instanceof Comparable) {
                    Comparable v1 = (Comparable)value1.toString();
                    Comparable v2 = (Comparable)value2.toString();

                    c = v1.compareTo(v2);
                    if (c != 0) return c;
                }
            }

            if (i.hasNext()) return 1;
            if (j.hasNext()) return -1;

        } finally {
            //System.out.println("Comparing "+this+" with "+object+": "+c);
        }

        return c;
    }
}