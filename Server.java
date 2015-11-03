import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.mina.core.service.IoAcceptor;  
import org.apache.mina.core.session.IdleStatus;  
import org.apache.mina.filter.codec.ProtocolCodecFilter;  
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;  
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;  
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.future.WriteFuture;

/**
 * This class represents a server for dispatching work to remote clients.
 */
public class Server implements Singleton
{  
    /** Should not be changed. */
    public static final int READ_BUFFER_SIZE = 2048;

    /** Should not be changed.  Time in seconds for a connection to be considered idle. */
    public static final int IDLE_TIME = 5;

    /** Listen for connections on this port. */
    public static final int LISTENING_PORT = Settings.LISTENING_PORT;

    /** How many threads to use for handling requests. */
    public static final int NUMBER_OF_THREADS = Settings.NUMBER_OF_THREADS;

    /** The hostname of the server. */
    public static final String HOSTNAME = Settings.HOSTNAME;

    /** The list of known clients. */
    private static final List<String> KNOWN_CLIENTS = new ArrayList<String>();

    /** Not instantiable. */
    private Server()
    {
        throw new IllegalArgumentException("not instantiable");
    }

    /** Start the server. */
    public static void start()
    {
        // use a thread pool
        ExecutorFilter executor = new ExecutorFilter(NUMBER_OF_THREADS, NUMBER_OF_THREADS);  // number of threads to start with, max number of threads
        
        // setup the connection
        IoAcceptor acceptor = new NioSocketAcceptor();
        acceptor.getSessionConfig().setReadBufferSize(READ_BUFFER_SIZE);  
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, IDLE_TIME);  
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
        acceptor.getFilterChain().addLast("executor1", executor);

        // use this adapter to listen for requests
        acceptor.setHandler(new ServerHandler());  
        
        // start accepting connections
        int attempts = 0;
        boolean success = false;
        while (attempts <= 10)
            {
                attempts++;
                try
                    {
                        acceptor.bind(new InetSocketAddress(LISTENING_PORT));  
                        System.out.printf("Server listening on port %d.\n", LISTENING_PORT);  
                        success = true;
                        break;
                    }
                catch (Exception e)
                    {
                        System.out.println("Unable to start server:");
                        e.printStackTrace();
                    }
            }
        if ( !success )
            {
                System.out.printf("Giving up after %d attempts, quitting.\n", attempts);
                System.exit(1);
            }
     }

    /** Quit when all current jobs are complete.  Currently implemented in a stupid way. */
    public static void waitForCompletion()
    {
        while (true)
            {
                if (WorkUnitDatabase.finished())
                    {
                        System.out.println("All jobs are complete.");
                        WorkUnitDatabase.printResults();
                        System.exit(0);
                    }
                try { Thread.sleep(500); }
                catch (InterruptedException e) {}
            }
    }

    /**
     * This class determines what happens when events pertaining to a connection occur.
     */
    public static class ServerHandler extends IoHandlerAdapter
    {
        public static String getHostname(IoSession session)
        {
            String name = (String)(session.getAttribute("hostname"));
            if ( name != null )
                return name;
            return "unknown";
        }

        public void messageReceived(IoSession session, Object message) throws Exception
        {
            if (message instanceof ResultEnvelope)
                {
                    ResultEnvelope envelope = (ResultEnvelope)message;
                    String remoteHostname = getHostname(session);
                    WorkUnitDatabase.receive(envelope, remoteHostname);
                    if ( envelope.errorMessage == null )
                        System.out.printf("Received work unit %d from %s.\n", envelope.serverID, remoteHostname);
                    else
                        System.out.printf("Received work unit %d from %s (FAILED : %s).\n", envelope.serverID, remoteHostname, envelope.errorMessage);
                    WorkUnitDatabase.sendOutWork(remoteHostname, session);
                }
            else if (message instanceof String)
                {
                    String name = (String)message;
                    synchronized (KNOWN_CLIENTS)
                        {
                            if (KNOWN_CLIENTS.contains(name))
                                {
                                    // deal with possible duplicate client names
                                    int count = 1;
                                    boolean success = false;
                                    while ( count < 1000 )
                                        {
                                            String candidate = String.format("%s-%d", name, count);
                                            if ( !KNOWN_CLIENTS.contains(candidate) )
                                                {
                                                    name = candidate;
                                                    success = true;
                                                    break;
                                                }
                                            count++;
                                        }
                                    if ( !success )
                                        throw new IllegalArgumentException("couldn't find unique name for " + name);
                                }
                            session.setAttribute("hostname", name);
                            KNOWN_CLIENTS.add(name);
                        }
                    int remoteThreads = Settings.getNumberOfThreads(name);
                    System.out.printf("Connected to client at %s (%s, %d threads).\n", name, session.getRemoteAddress(), remoteThreads);
                    
                    // send the initial batch of jobs
                    for (int i=0; i < remoteThreads; i++)
                        WorkUnitDatabase.sendOutWork(name, session);
                }
            else
                throw new IllegalArgumentException("unrecognized object type");
        }  
          
        public void sessionOpened(IoSession session) throws Exception
        {
            // handshake by sending a string that contains the name of this host
            session.write(HOSTNAME);
        }

        public void sessionIdle(IoSession session, IdleStatus status)
        {
            String remoteHostname = getHostname(session);
            WorkUnitDatabase.sendOutWork(remoteHostname, session);
        }

        public void sessionClosed(IoSession session) throws Exception
        {
            String remoteHostname = getHostname(session);
            System.out.printf("Lost connection to %s.\n", remoteHostname);
            WorkUnitDatabase.markAsDead(remoteHostname);
            synchronized (KNOWN_CLIENTS)
                {
                    KNOWN_CLIENTS.remove(remoteHostname);
                }
        }
    }

    /** For testing. */
    public static void main(String[] args) throws IOException
    {
        for (int i=0; i < 100; i++)
            {
                DummyWorkUnit unit = new DummyWorkUnit(i==5); // make unit 6 fail
                WorkEnvelope workEnvelope = new WorkEnvelope(unit);
                WorkUnitDatabase.submit(workEnvelope);
            }
        Server.start();
        Server.waitForCompletion();
    }  
}
