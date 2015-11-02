import java.net.*;
import java.io.*;

import org.apache.mina.core.service.IoConnector;  
import org.apache.mina.filter.codec.ProtocolCodecFilter;  
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;  
import org.apache.mina.transport.socket.nio.NioSocketConnector;  
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;

public class Client
{  
    public static final String NAME;

    static
    {
        // set hostname
        String temp = "";
        try
            {
                temp = java.net.InetAddress.getLocalHost().getHostName();
            }
        catch (Exception e)
            {
                System.out.println("Warning, unable to detect hostname.  Using localhost.");
                temp = "localhost";
            }
        NAME = temp;
    }

    public static void main(String[] args)
    {  
        IoConnector connector = new NioSocketConnector();  
        connector.setConnectTimeoutMillis(3000);  
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
        connector.setHandler(new ClientHandler());  
        IoSession session = null;
        try
            {
                ConnectFuture future = connector.connect(new InetSocketAddress("127.0.0.1", 9876));  
                future.awaitUninterruptibly();
                System.out.println("Connected to server.");
                session = future.getSession();
            }
        catch (Exception e)
            {
                System.out.println("Failed to connect:");
                e.printStackTrace();
            }
    }  
} 
