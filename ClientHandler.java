import org.apache.mina.core.service.IoHandlerAdapter;  
import org.apache.mina.core.session.IoSession;  

public class ClientHandler extends IoHandlerAdapter
{  
    private final UserInfo ui;  
      
    public ClientHandler(UserInfo ui)
    {  
        this.ui = ui;  
    }  
  
    public void sessionOpened(IoSession session) throws Exception
    {  
        session.write(ui);  
    }  
      
    public void messageReceived(IoSession session, Object message) throws Exception
    {  
        UserInfo ui = (UserInfo)message;  
        System.out.println("message received: " + ui.string);  
    }  
  
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception
    {  
        System.out.println(session.getRemoteAddress() + ":[" + cause.getMessage() + "]");  
        session.close(false);  
        session.getService().dispose();  
    }  
}
