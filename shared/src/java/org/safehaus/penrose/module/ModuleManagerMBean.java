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
package org.safehaus.penrose.module;

import java.util.Collection;

/**
 * @author Endi S. Dewata
 */
public interface ModuleManagerMBean {

    public final static String NAME = "Penrose:name=ModuleManager";

    public Collection getPartitionNames() throws Exception;
    public Collection getModuleNames(String partitionName) throws Exception;
    public ModuleConfig getModuleConfig(String partitionName, String moduleName) throws Exception;
    public String getStatus(String partitionName, String moduleName) throws Exception;

    public void start(String partitionName, String moduleName) throws Exception;
    public void stop(String partitionName, String moduleName) throws Exception;
    public void restart(String partitionName, String moduleName) throws Exception;
}