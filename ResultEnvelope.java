import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A wrapper for work unit results (Result).  These can be sent over the network.
 */
public class ResultEnvelope implements Serializable, Immutable
{
    /** For serialization. */
    public static final long serialVersionUID = 1L;

    /** For assigning unique IDs. */
    public static final AtomicLong ID_GENERATOR = new AtomicLong();

    /** The work that will be performed. */
    public final Result result;

    /** Where this envelope was made. */
    public final String origin;

    /** A unique identifier. */
    public final long serverID;

    /** Create a ResultEnvelope with a particular serverID. */
    public ResultEnvelope(Result result, String origin, long serverID)
    {
        if ( result == null )
            throw new NullPointerException("null result");
        if ( origin == null )
            throw new NullPointerException("null origin");
        this.result = result;
        this.origin = origin;
        this.serverID = serverID;
    }

    @Override
    public String toString()
    {
        return String.format("ResultEnvelope %d", serverID);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(result, origin, serverID);
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null )
            return false;
        if ( obj == this )
            return true;
        if ( !(obj instanceof ResultEnvelope) )
            return false;

        ResultEnvelope e = (ResultEnvelope)obj;
        if ( Objects.equals(result, e.result) &&
             Objects.equals(origin, e.origin) &&
             serverID == e.serverID )
            return true;
        return false;
    }    
}
