package org.safehaus.penrose.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Endi S. Dewata
 */
public class EngineMapping implements Cloneable {

    Logger log = LoggerFactory.getLogger(getClass());

	private String name = "DEFAULT";

    private String engineName;

    private Properties parameters = new Properties();

	public EngineMapping() {
	}

    public EngineMapping(String name, String engineName) {
        this.name = name;
        this.engineName = engineName;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getParameter(String name) {
        return parameters.getProperty(name);
    }

    public void removeParameters() {
        parameters.clear();
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public void removeParameter(String name) {
        parameters.remove(name);
    }

    public Collection getParameterNames() {
        return parameters.keySet();
    }

    public int hashCode() {
        return (name == null ? 0 : name.hashCode()) +
                (engineName == null ? 0 : engineName.hashCode()) +
                (parameters == null ? 0 : parameters.hashCode());
    }

    boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) return true;
        if (o1 != null) return o1.equals(o2);
        return o2.equals(o1);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if((object == null) || (object.getClass() != this.getClass())) return false;

        EngineMapping engineMapping = (EngineMapping)object;
        if (!equals(name, engineMapping.name)) return false;
        if (!equals(engineName, engineMapping.engineName)) return false;
        if (!equals(parameters, engineMapping.parameters)) return false;

        return true;
    }

    public void copy(EngineMapping engineMapping) {
        name = engineMapping.name;
        engineName = engineMapping.engineName;

        removeParameters();
        parameters.putAll(engineMapping.parameters);
    }

    public Object clone() {
        EngineMapping engineMapping = new EngineMapping();
        engineMapping.copy(this);
        return engineMapping;
    }
}
