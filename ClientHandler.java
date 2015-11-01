import org.apache.mina.core.service.IoHandlerAdapter;  
import org.apache.mina.core.session.IoSession;  

public class ClientHandler extends IoHandlerAdapter
{  
    public ClientHandler()
    {  
    }  
  
    public void sessionCreated(IoSession session) throws Exception
    {
        System.out.println("Session created.");
    }

    public void messageReceived(IoSession session, Object message) throws Exception
    {  
        if (message instanceof RemoteWorkUnit)
            {
                RemoteWorkUnit unit = (RemoteWorkUnit)message;
                System.out.printf("Received work unit %d.\n", unit.getServerID());
                WrappedUnit wrappedUnit = new WrappedUnit(unit, session);
                GeneralThreadService.submit(wrappedUnit);
            }
        else
            throw new IllegalArgumentException("unexpected object type");
    }  

    public void sessionClosed(IoSession session)
    {
        System.out.println("Session closed.");
        System.exit(0);
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception
    {  
        System.out.println(session.getRemoteAddress() + ":[" + cause.getMessage() + "]");  
        session.close(false);  
        session.getService().dispose();  
    }  

    public static class WrappedUnit implements WorkUnit
    {
        public final RemoteWorkUnit unit;
        public final IoSession session;

        public WrappedUnit(RemoteWorkUnit unit, IoSession session)
        {
            this.unit = unit;
            this.session = session;
        }

        public Result call()
        {
            RemoteResult result = unit.call();
            session.write(result);
            System.out.printf("Finished work unit %d.\n", unit.getServerID());
            return null;
        }
    }
}
