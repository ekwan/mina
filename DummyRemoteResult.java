import java.io.*;

public class DummyRemoteResult implements Serializable, RemoteResult
{
    public static final long serialVersionUID = 1L;
    public final long serverID;

    public DummyRemoteResult(long serverID)
    {
        this.serverID = serverID;
    }

    public long getServerID()
    {
        return serverID;
    }
}
