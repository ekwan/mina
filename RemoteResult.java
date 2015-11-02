import java.io.*;

public interface RemoteResult extends Serializable
{
    /** For serialization. */
    public static final long serialVersionUID = 1L;

    /** See {@link RemoteWorkUnit#getServerID()}. */
    public long getServerID();
    
    public String getOrigin();

    @Override
    public String toString();
}
