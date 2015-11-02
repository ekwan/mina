import java.io.*;

public class DummyRemoteResult implements Serializable, RemoteResult
{
    public static final long serialVersionUID = 1L;
    public final long serverID;
    public final String origin;

    public DummyRemoteResult(long serverID, String origin)
    {
        this.serverID = serverID;
        this.origin = origin;
    }

    public long getServerID()
    {
        return serverID;
    }

    public String getOrigin()
    {
        return origin;
    }
}
