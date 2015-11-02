import org.apache.mina.core.service.IoHandlerAdapter;  
import org.apache.mina.core.session.IoSession;  
import org.apache.mina.core.future.WriteFuture;
import java.util.concurrent.*;

public class ServerHandler extends IoHandlerAdapter
{
    public void messageReceived(IoSession session, Object message) throws Exception
    {
        if (message instanceof RemoteResult)
            {
                RemoteResult result = (RemoteResult)message;
                System.out.printf("Received work unit %d from %s.\n", result.getServerID(), result.getOrigin());
                Server.FINISHED.add(result);
                if ( Server.FINISHED.size() == Server.NUMBER_OF_UNITS )
                    {
                        System.out.println("All work is complete.");
                        System.exit(0);
                    }
                sendWork(session);
            }
        else
            throw new IllegalArgumentException("unrecognized object type");
    }  
      
    public void sessionOpened(IoSession session) throws Exception
    {  
        System.out.println("Opened session with " + session.getRemoteAddress());
        for (int i=0; i < GeneralThreadService.NUMBER_OF_THREADS; i++)
            sendWork(session);
    }  

    private void sendWork(IoSession session)
    {
        RemoteWorkUnit unit = Server.WORK_LIST.poll();
        if ( unit == null )
            return;
        WriteFuture future = session.write(unit);
        System.out.printf("Tried to send out work unit %d.\n", unit.getServerID());
    }
}
