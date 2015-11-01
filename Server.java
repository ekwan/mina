import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.mina.core.service.IoAcceptor;  
import org.apache.mina.core.session.IdleStatus;  
import org.apache.mina.filter.codec.ProtocolCodecFilter;  
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;  
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;  

public class Server
{  
    public static final ConcurrentLinkedQueue<RemoteWorkUnit> WORK_LIST = new ConcurrentLinkedQueue<>();
    public static final List<RemoteResult> FINISHED = Collections.synchronizedList(new ArrayList<RemoteResult>());
    public static final int NUMBER_OF_UNITS = 10;

    public static void main(String[] args) throws IOException
    {  
        int bindPort = 9876;  

        // create work
        for (int i=0; i < NUMBER_OF_UNITS; i++)
            WORK_LIST.add(new DummyRemoteWorkUnit((long)i));

        IoAcceptor acceptor = new NioSocketAcceptor();  
        acceptor.getSessionConfig().setReadBufferSize(2048);  
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);  
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
        acceptor.setHandler(new ServerHandler());  
        acceptor.bind(new InetSocketAddress(bindPort));  
          
        System.out.println("Server listening on port " + bindPort);  
    }  
}  
