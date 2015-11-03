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

    /** If true, this unit will error out. */
    public final boolean throwError;

    public DummyWorkUnit(boolean throwError)
    {
        this.throwError = throwError;
    }

    public DummyResult call()
    {
        if (throwError)
            throw new IllegalArgumentException("this is a contrived error");
        try
            {
                int time = ThreadLocalRandom.current().nextInt(MEAN_TIME-WIDTH, MEAN_TIME+WIDTH+1);
                //System.out.printf("Waiting for %d seconds.\n", time);
                Thread.sleep(time*1000);
            }
        catch (InterruptedException e)
            {
            }
        return new DummyResult();
    }
}
