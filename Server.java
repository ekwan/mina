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

public class Server
{  
    public static final ConcurrentLinkedQueue<RemoteWorkUnit> WORK_LIST = new ConcurrentLinkedQueue<>();
    public static final List<RemoteResult> FINISHED = Collections.synchronizedList(new ArrayList<RemoteResult>());
    public static final int NUMBER_OF_UNITS = 30;

    public static void main(String[] args) throws IOException
    {  
        int bindPort = 9876;  

        // create work
        for (int i=0; i < NUMBER_OF_UNITS; i++)
            WORK_LIST.add(new DummyRemoteWorkUnit((long)i));

        // use a thread pool
        ExecutorFilter executor = new ExecutorFilter(2, 4);  // number of threads to start with, max number of threads

        IoAcceptor acceptor = new NioSocketAcceptor();  
        acceptor.getSessionConfig().setReadBufferSize(2048);  
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);  
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));  
        acceptor.getFilterChain().addLast("executor1", executor);
        acceptor.setHandler(new ServerHandler());  
        acceptor.bind(new InetSocketAddress(bindPort));  
          
        System.out.println("Server listening on port " + bindPort);  
    }  
}  
