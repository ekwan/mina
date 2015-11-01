import org.apache.mina.core.service.IoHandlerAdapter;  
import org.apache.mina.core.session.IoSession;  
  
public class ServerHandler extends IoHandlerAdapter
{
    public void messageReceived(IoSession session, Object message) throws Exception
    {
        UserInfo ui = (UserInfo)message; 
        System.out.println("Received message: " + ui.string);  
        session.write(new UserInfo("acknowledged"));
    }  
      
    public void sessionOpened(IoSession session) throws Exception
    {  
        System.out.println("Incoming Client: " + session.getRemoteAddress());  
    }  
}  
