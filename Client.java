import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import org.apache.mina.core.service.IoConnector;  
import org.apache.mina.filter.codec.ProtocolCodecFilter;  
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;  
import org.apache.mina.transport.socket.nio.NioSocketConnector;  
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.service.IoHandlerAdapter;  
import org.apache.mina.core.session.IoSession;  

public class Client implements Singleton
{ 
    /** The connection to the server. */
    private static IoSession IO_SESSION;

    /** Static initializer. */
    static
    {
        int attempts = 0;
        boolean connected = false;
        while (attempts < Settings.MAX_CONNECTION_ATTEMPTS)
            {
                IoConnector connector = new NioSocketConnector();  
                connector.setConnectTimeoutMillis(3000);  
                connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
                connector.setHandler(new ClientHandler());  
                try
                    {
                        ConnectFuture future = connector.connect(new InetSocketAddress(Settings.SERVER_HOSTNAME, Settings.LISTENING_PORT));  
                        future.awaitUninterruptibly();
                        IO_SESSION = future.getSession();
                        System.out.printf("Connected to server (%s:%d).\n", Settings.SERVER_HOSTNAME, Settings.LISTENING_PORT);
                        connected = true;
                        break;
                    }
                catch (Exception e)
                    {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String message = sw.toString();
                        if ( message.toLowerCase().contains("connection refused") )
                            System.out.println("Connection refused.");
                        else
                            {
                                System.out.println("Failed to connect:");
                                e.printStackTrace();
                            }
                        System.out.printf("Waiting before retrying...");
                        try { Thread.sleep(Settings.CONNECTION_RETRY_DELAY*1000); }
                        catch (Exception e2) {}
                        System.out.println("done waiting.");
                    }
                attempts++;
            }
        if ( !connected )
            System.out.println("Maximum connection attempts exceeded.");
    }

    /** Not instantiable. */
    private Client()
    {
        throw new IllegalArgumentException("not instantiable");
    }

    /**
     * This class determines what happens when events pertaining to a connection occur.
     */
    public static class ClientHandler extends IoHandlerAdapter
    {
        private String serverHostname = "unknown";

        public ClientHandler()
        {  
        }  
      
        public void sessionCreated(IoSession session) throws Exception
        {
            System.out.println("Session created.");
            session.write(Settings.HOSTNAME);
        }

        public void messageReceived(IoSession session, Object message) throws Exception
        {  
            if (message instanceof WorkEnvelope)
                {
                }
            else if (message instanceof String)
                {
                    serverHostname = (String)message;
                    System.out.printf("Connected to server at %s.\n", serverHostname);
                }
            else
                throw new IllegalArgumentException("unexpected object type");
        }  

        public void sessionClosed(IoSession session)
        {
            System.out.println("Connection to server closed.");
            session.getService().dispose();
            System.exit(0);
        }

        public void exceptionCaught(IoSession session, Throwable cause) throws Exception
        {  
            System.out.println(session.getRemoteAddress() + ":[" + cause.getMessage() + "]");  
            session.close(false);  
            session.getService().dispose();  
        }  
    }

    /** For testing. */
    public static void main(String[] args)
    {  
    }  
} 
