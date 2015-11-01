import java.net.InetSocketAddress;  
  
import org.apache.mina.core.service.IoConnector;  
import org.apache.mina.filter.codec.ProtocolCodecFilter;  
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;  
import org.apache.mina.transport.socket.nio.NioSocketConnector;  
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
public class Client
{  
    public static void main(String[] args)
    {  
        IoConnector connector = new NioSocketConnector();  
        connector.setConnectTimeoutMillis(3000);  
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
        connector.setHandler(new ClientHandler(new UserInfo("hello I am the client")));  
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
        while (true)
            {
                String input = System.console().readLine();
                if ( input.toLowerCase().equals("quit") )
                    {
                        System.out.println("Quitting.");
                        break;
                    }
                session.write(new UserInfo(input));
            }
    }  
} 
