import java.util.*;
import java.io.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This is a WorkUnit that can be used for testing the server-client paradigm.
 * All it does is wait a while and then return.
 */
public class DummyWorkUnit implements Immutable, WorkUnit, Serializable
{
    /** For serialization. */
    public static final long serialVersionUID = 1L;

    /** How long each job will take on average in seconds. */
    public static final int MEAN_TIME = 4;

    /** The time required for each job is the mean plus or minus this value in seconds. */
    public static final int WIDTH = 1;

    public DummyWorkUnit()
    {
    }

    public DummyResult call()
    {
        try
            {
                int time = ThreadLocalRandom.current().nextInt(MEAN_TIME-WIDTH, MEAN_TIME+WIDTH+1);
                Thread.sleep(time*1000);
            }
        catch (InterruptedException e)
            {
            }
        return new DummyResult();
    }
}
