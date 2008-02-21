package org.safehaus.penrose.management;

import org.safehaus.penrose.connection.ConnectionConfig;

/**
 * @author Endi Sukma Dewata
 */
public interface ConnectionServiceMBean {

    public void start() throws Exception;
    public void stop() throws Exception;
    public void restart() throws Exception;
    
    public ConnectionConfig getConnectionConfig() throws Exception;
}