import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

/**
 * A wrapper for work units.  These can be sent over the network.
 */
public class WorkEnvelope implements Serializable, Immutable
{
    /** For serialization. */
    public static final long serialVersionUID = 1L;

    /** For assigning unique IDs. */
    public static final AtomicLong ID_GENERATOR = new AtomicLong();

    /** The work that will be performed. */
    public final WorkUnit workUnit;

    /** Where this envelope was made. */
    public final String origin;

    /** A unique identifier. */
    public final long serverID;

    /** Create a WorkEnvelope with a particular serverID. */
    public WorkEnvelope(WorkUnit workUnit, String origin, long serverID)
    {
        if ( workUnit == null )
            throw new NullPointerException("null work unit");
        if ( origin == null )
            throw new NullPointerException("null origin");
        this.workUnit = workUnit;
        this.origin = origin;
        this.serverID = serverID;
    }

    /** Create a WorkEnvelope with an auto-generated serverID. */
    public WorkEnvelope(WorkUnit workUnit, String origin)
    {
        this(workUnit, origin, ID_GENERATOR.incrementAndGet());
    }

    /** The easiest way to create a WorkEnvelope. */
    public WorkEnvelope(WorkUnit workUnit)
    {
        this(workUnit, Settings.HOSTNAME, ID_GENERATOR.incrementAndGet());
    }

    /**
     * Performs the work in this envelope and creates a result that can be sent back.
     * Submits the work to the executor service and then executes a callback that returns the work to the server.
     */
    public void run()
    {
    }

    @Override
    public String toString()
    {
        return String.format("WorkEnvelope %d", serverID);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(workUnit, origin, serverID);
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null )
            return false;
        if ( obj == this )
            return true;
        if ( !(obj instanceof WorkEnvelope) )
            return false;

        WorkEnvelope e = (WorkEnvelope)obj;
        if ( Objects.equals(workUnit, e.workUnit) &&
             Objects.equals(origin, e.origin) &&
             serverID == e.serverID )
            return true;
        return false;
    }    
}
