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
    // Constants

    /** Should not be changed. */
    public static final int READ_BUFFER_SIZE = 2048;

    /** Should not be changed.  Time in seconds. */
    public static final int IDLE_TIME = 10;

    /** Listen for connections on this port. */
    public static final int LISTENING_PORT = Settings.LISTENING_PORT;

    /** How many threads to use for handling requests. */
    public static final int NUMBER_OF_THREADS = Settings.NUMBER_OF_THREADS;

    /** The hostname of the server. */
    public static final String HOSTNAME = Settings.HOSTNAME;

    // Fields

    /** The work to be performed. */
    public static final ConcurrentLinkedQueue<WorkUnit> WORK_LIST = new ConcurrentLinkedQueue<WorkUnit>();

    /** Not instantiable. */
    private Server()
    {
        throw new IllegalArgumentException("not instantiable");
    }

    /** Static initializer. */
    static
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
        try
            {
                acceptor.bind(new InetSocketAddress(LISTENING_PORT));  
                System.out.printf("Server listening on port %d.\n", LISTENING_PORT);  
            }
        catch (Exception e)
            {
                System.out.println("Unable to start server:");
                e.printStackTrace();
                System.exit(1);
            }
     }
    
    /**
     * This class determines what happens when events pertaining to a connection occur.
     */
    public static class ServerHandler extends IoHandlerAdapter
    {
        public String remoteHostname = "unknown";

        public void messageReceived(IoSession session, Object message) throws Exception
        {
            if (message instanceof ResultEnvelope)
                {
                }
            else if (message instanceof String)
                {
                    remoteHostname = (String)message;
                    int remoteThreads = Settings.getNumberOfThreads(remoteHostname);
                    System.out.printf("Connected to client at %s (%s, %d threads).\n", remoteHostname, session.getRemoteAddress(), remoteThreads);
                    for (int i=0; i < remoteThreads; i++)
                        sendWork(session);
                }
            else
                throw new IllegalArgumentException("unrecognized object type");
        }  
          
        public void sessionOpened(IoSession session) throws Exception
        {
            // handshake by sending a string that contains the name of this host
            session.write(HOSTNAME);
        }  

        private void sendWork(IoSession session)
        {
            WorkUnit workUnit = Server.WORK_LIST.poll();
            if ( workUnit == null )
                return;
            WorkEnvelope workEnvelope = new WorkEnvelope(workUnit);
            WriteFuture future = session.write(workEnvelope);
            System.out.printf("Sent work unit %d to %s.\n", workEnvelope.serverID, remoteHostname);
        }
    }

    /** For testing. */
    public static void main(String[] args) throws IOException
    {  
    }  
}
