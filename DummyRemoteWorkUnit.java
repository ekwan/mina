import java.util.*;
import java.io.*;

public class DummyRemoteWorkUnit implements RemoteWorkUnit, Serializable
{
    public static final long serialVersionUID = 1L;
    public final long serverID;

    public DummyRemoteWorkUnit(long serverID)
    {
        this.serverID = serverID;
    }

    public DummyRemoteResult call()
    {
        try
            {
                Thread.sleep(3000);
            }
        catch (InterruptedException e)
            {
            }
        return new DummyRemoteResult(serverID, Client.NAME);
    }

    public long getServerID()
    {
        return serverID;
    }
}
