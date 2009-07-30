package org.safehaus.penrose.ipa;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.safehaus.penrose.client.PenroseClient;
import org.safehaus.penrose.module.ModuleClient;
import org.safehaus.penrose.module.ModuleManagerClient;
import org.safehaus.penrose.partition.PartitionClient;
import org.safehaus.penrose.partition.PartitionManagerClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Endi Sukma Dewata
 */
public class IPAClient {

    PenroseClient client;

    public IPAClient(
            String serverType,
            String protocol,
            String hostname,
            int port,
            String bindDn,
            String bindPassword
    ) throws Exception {

        client = new PenroseClient(
                serverType,
                protocol,
                hostname,
                port,
                bindDn,
                bindPassword
        );
    }

    public void setRmiTransportPort(int rmiTransportPort) {
        client.setRmiTransportPort(rmiTransportPort);
    }

    public void connect() throws Exception {
        client.connect();
    }

    public void close() throws Exception {
        client.close();
    }


    public void execute(String partition, Collection<String> parameters) throws Exception {

        Iterator<String> iterator = parameters.iterator();

        String command = iterator.next();
        if ("sync".equals(command)) {
            syncAll(partition);

        } else if ("sync-users".equals(command)) {
            syncUsers(partition);

        } else if ("sync-ipa-users".equals(command)) {
            syncIPAUsers(partition);

        } else if ("sync-ipa-user".equals(command)) {
            String key = iterator.next();
            syncIPAUser(partition, key);

        } else if ("unlink-ipa-user".equals(command)) {
            String key = iterator.next();
            unlinkIPAUser(partition, key);

        } else if ("delete-ipa-user".equals(command)) {
            String key = iterator.next();
            deleteIPAUser(partition, key);

        } else if ("sync-samba-users".equals(command)) {
            syncSambaUsers(partition);

        } else if ("sync-samba-user".equals(command)) {
            String key = iterator.next();
            syncSambaUser(partition, key);

        } else if ("unlink-samba-user".equals(command)) {
            String key = iterator.next();
            unlinkSambaUser(partition, key);

        } else if ("delete-samba-user".equals(command)) {
            String key = iterator.next();
            deleteSambaUser(partition, key);

        } else if ("sync-groups".equals(command)) {
            syncGroups(partition);

        } else if ("sync-ipa-groups".equals(command)) {
            syncIPAGroups(partition);

        } else if ("sync-ipa-group".equals(command)) {
            String key = iterator.next();
            syncIPAGroup(partition, key);

        } else if ("unlink-ipa-group".equals(command)) {
            String key = iterator.next();
            unlinkIPAGroup(partition, key);

        } else if ("delete-ipa-group".equals(command)) {
            String key = iterator.next();
            deleteIPAGroup(partition, key);

        } else if ("sync-samba-groups".equals(command)) {
            syncSambaGroups(partition);

        } else if ("sync-samba-group".equals(command)) {
            String key = iterator.next();
            syncSambaGroup(partition, key);

        } else if ("unlink-samba-group".equals(command)) {
            String key = iterator.next();
            unlinkSambaGroup(partition, key);

        } else if ("delete-samba-group".equals(command)) {
            String key = iterator.next();
            deleteSambaGroup(partition, key);

        } else if ("sync-hosts".equals(command)) {
            syncHosts(partition);

        } else {
            throw new Exception("Unknown command: "+command);
        }
    }

    public void syncAll(String partition) throws Exception {
        syncUsers(partition);
        syncGroups(partition);
        syncHosts(partition);
    }

    public void syncUsers(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPAUserModule");

        moduleClient.invoke("syncUsers");
    }

    public void syncIPAUsers(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaUserModule");

        moduleClient.invoke("syncUsers");
    }

    public void syncIPAUser(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaUserModule");

        moduleClient.invoke(
                "syncUser",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void unlinkIPAUser(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaUserModule");

        moduleClient.invoke(
                "unlinkUser",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void deleteIPAUser(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaUserModule");

        moduleClient.invoke(
                "deleteUser",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void syncSambaUsers(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAUserModule");

        moduleClient.invoke("syncUsers");
    }

    public void syncSambaUser(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAUserModule");

        moduleClient.invoke(
                "syncUser",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void unlinkSambaUser(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAUserModule");

        moduleClient.invoke(
                "unlinkUser",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void deleteSambaUser(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAUserModule");

        moduleClient.invoke(
                "deleteUser",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void syncGroups(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPAGroupModule");

        moduleClient.invoke("syncGroups");
    }

    public void syncIPAGroups(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaGroupModule");

        moduleClient.invoke("syncGroups");
    }

    public void syncIPAGroup(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaGroupModule");

        moduleClient.invoke(
                "syncGroup",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void unlinkIPAGroup(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaGroupModule");

        moduleClient.invoke(
                "unlinkGroup",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void deleteIPAGroup(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPASambaGroupModule");

        moduleClient.invoke(
                "deleteGroup",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void syncSambaGroups(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAGroupModule");

        moduleClient.invoke("syncGroups");
    }

    public void syncSambaGroup(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAGroupModule");

        moduleClient.invoke(
                "syncGroup",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void unlinkSambaGroup(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAGroupModule");

        moduleClient.invoke(
                "unlinkGroup",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void deleteSambaGroup(String partition, String key) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("SambaIPAGroupModule");

        moduleClient.invoke(
                "deleteGroup",
                new Object[] { key },
                new String[] { String.class.getName() }
        );
    }

    public void syncHosts(String partition) throws Exception {
        PartitionManagerClient partitionManagerClient = client.getPartitionManagerClient();
        PartitionClient partitionClient = partitionManagerClient.getPartitionClient(partition);
        ModuleManagerClient moduleManagerClient = partitionClient.getModuleManagerClient();
        ModuleClient moduleClient = moduleManagerClient.getModuleClient("IPAModule");

        moduleClient.invoke("syncHosts");
    }

    public static void showUsage() {
        System.out.println("Usage: org.safehaus.penrose.ipa.IPAClient [OPTION]... <command> [arguments]...");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -?, --help         display this help and exit");
        System.out.println("  -P protocol        Penrose JMX protocol");
        System.out.println("  -h host            Penrose server");
        System.out.println("  -p port            Penrose JMX port");
        System.out.println("  -D username        username");
        System.out.println("  -w password        password");
        System.out.println("  -d                 run in debug mode");
        System.out.println("  -v                 run in verbose mode");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  sync                      Sync all.");
        System.out.println("  sync-users                Sync all users.");
        System.out.println("  sync-ipa-users            Sync all IPA users.");
        System.out.println("  sync-ipa-user <key>       Sync IPA user.");
        System.out.println("  unlink-ipa-user <key>     Unlink IPA user.");
        System.out.println("  delete-ipa-user <key>     Delete IPA user.");
        System.out.println("  sync-samba-users          Sync all Samba users.");
        System.out.println("  sync-samba-user <key>     Sync Samba user.");
        System.out.println("  unlink-samba-user <key>   Unlink Samba user.");
        System.out.println("  delete-samba-user <key>   Delete Samba user.");
        System.out.println("  sync-groups               Sync all groups.");
        System.out.println("  sync-ipa-groups           Sync all IPA groups.");
        System.out.println("  sync-ipa-group <key>      Sync IPA group.");
        System.out.println("  unlink-ipa-group <key>    Unlink IPA group.");
        System.out.println("  delete-ipa-group <key>    Delete IPA group.");
        System.out.println("  sync-samba-groups         Sync all Samba group.");
        System.out.println("  sync-samba-group <key>    Sync Samba group.");
        System.out.println("  unlink-samba-group <key>  Unlink Samba group.");
        System.out.println("  delete-samba-group <key>  Delete Samba group.");
        System.out.println("  sync-hosts                Sync hosts.");
        System.out.println("  sync-ipa-host <key>       Sync IPA host.");
        System.out.println("  sync-samba-host <key>     Sync Samba host.");
    }

    public static void main(String args[]) throws Exception {

        Level level          = Level.WARN;
        String serverType    = PenroseClient.PENROSE;
        String protocol      = PenroseClient.DEFAULT_PROTOCOL;
        String hostname      = "localhost";
        int portNumber       = PenroseClient.DEFAULT_RMI_PORT;
        int rmiTransportPort = PenroseClient.DEFAULT_RMI_TRANSPORT_PORT;

        String bindDn = null;
        String bindPassword = null;

        LongOpt[] longopts = new LongOpt[1];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, '?');

        Getopt getopt = new Getopt("IPAClient", args, "-:?dvt:h:p:r:P:D:w:", longopts);

        Collection<String> parameters = new ArrayList<String>();
        int c;
        while ((c = getopt.getopt()) != -1) {
            switch (c) {
                case ':':
                case '?':
                    showUsage();
                    System.exit(0);
                    break;
                case 1:
                    parameters.add(getopt.getOptarg());
                    break;
                case 'd':
                    level = Level.DEBUG;
                    break;
                case 'v':
                    level = Level.INFO;
                    break;
                case 'P':
                    protocol = getopt.getOptarg();
                    break;
                case 't':
                    serverType = getopt.getOptarg();
                    break;
                case 'h':
                    hostname = getopt.getOptarg();
                    break;
                case 'p':
                    portNumber = Integer.parseInt(getopt.getOptarg());
                    break;
                case 'r':
                    rmiTransportPort = Integer.parseInt(getopt.getOptarg());
                    break;
                case 'D':
                    bindDn = getopt.getOptarg();
                    break;
                case 'w':
                    bindPassword = getopt.getOptarg();
            }
        }

        if (parameters.size() == 0) {
            showUsage();
            System.exit(0);
        }

        File penroseHome = new File(System.getProperty("penrose.home"));

        //Logger rootLogger = Logger.getRootLogger();
        //rootLogger.setLevel(Level.OFF);

        Logger logger = Logger.getLogger("org.safehaus.penrose");

        File log4jXml = new File(penroseHome, "conf"+File.separator+"log4j.xml");

        if (level.equals(Level.DEBUG)) {
            logger.setLevel(level);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-20C{1} [%4L] %m%n"));
            BasicConfigurator.configure(appender);

        } else if (level.equals(Level.INFO)) {
            logger.setLevel(level);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
            BasicConfigurator.configure(appender);

        } else if (log4jXml.exists()) {
            DOMConfigurator.configure(log4jXml.getAbsolutePath());

        } else {
            logger.setLevel(level);
            ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%d{MM/dd/yyyy HH:mm:ss}] %m%n"));
            BasicConfigurator.configure(appender);
        }

        try {
            IPAClient client = new IPAClient(
                    serverType,
                    protocol,
                    hostname,
                    portNumber,
                    bindDn,
                    bindPassword
            );

            client.setRmiTransportPort(rmiTransportPort);
            client.connect();

            String partition = System.getProperty("partition.name");
            client.execute(partition, parameters);

            client.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}