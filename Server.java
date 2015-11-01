import java.io.*;
import java.net.*;

import org.apache.mina.core.service.IoAcceptor;  
import org.apache.mina.core.session.IdleStatus;  
import org.apache.mina.filter.codec.ProtocolCodecFilter;  
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;  
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;  

public class Server
{  
    public static void main(String[] args) throws IOException
    {  
        int bindPort = 9876;  
          
        IoAcceptor acceptor = new NioSocketAcceptor();  
        acceptor.getSessionConfig().setReadBufferSize(2048);  
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);  
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
        acceptor.setHandler(new ServerHandler());  
        acceptor.bind(new InetSocketAddress(bindPort));  
          
        System.out.println("Server listening on port " + bindPort);  
    }  
}  
