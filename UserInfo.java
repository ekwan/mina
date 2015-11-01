import java.io.*;

public class UserInfo implements Serializable
{
    public static final long serialVersionUID = 1L;
    public final String string;

    public UserInfo(String string)
    {
        this.string = string;
    }
}
