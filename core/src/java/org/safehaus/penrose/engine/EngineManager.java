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

import org.safehaus.penrose.config.PenroseConfig;
import org.safehaus.penrose.Penrose;
import org.safehaus.penrose.naming.PenroseContext;
import org.apache.log4j.Logger;

import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;

/**
 * @author Endi S. Dewata
 */
public class EngineManager {

    Logger log = Logger.getLogger(getClass());

    private PenroseConfig penroseConfig;
    private PenroseContext penroseContext;

    Map engines = new TreeMap();

    public EngineManager() {
    }

    public void init(EngineConfig engineConfig) throws Exception {

        log.debug("Initializing engine "+engineConfig.getName()+".");

        Class clazz = Class.forName(engineConfig.getEngineClass());

        Engine engine = (Engine)clazz.newInstance();

        engine.setEngineConfig(engineConfig);
        engine.setPenroseConfig(penroseConfig);
        engine.setPenroseContext(penroseContext);
        engine.init();

        engines.put(engineConfig.getName(), engine);
    }

    public Engine getEngine(String name) {
        return (Engine)engines.get(name);
    }

    public void clear() {
        engines.clear();
    }

    public void start() throws Exception {
        for (Iterator i=engines.values().iterator(); i.hasNext(); ) {
            Engine engine = (Engine)i.next();
            engine.start();
        }
    }

    public void stop() throws Exception {
        for (Iterator i=engines.values().iterator(); i.hasNext(); ) {
            Engine engine = (Engine)i.next();
            engine.stop();
        }
    }

    public PenroseConfig getPenroseConfig() {
        return penroseConfig;
    }

    public void setPenroseConfig(PenroseConfig penroseConfig) {
        this.penroseConfig = penroseConfig;
    }

    public PenroseContext getPenroseContext() {
        return penroseContext;
    }

    public void setPenroseContext(PenroseContext penroseContext) {
        this.penroseContext = penroseContext;
    }
}
