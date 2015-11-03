import com.google.common.util.concurrent.*;
import com.google.common.collect.*;
import java.util.concurrent.*;
import java.util.*;
import org.apache.mina.core.session.IoSession;

/**
 * Runs WorkUnits.
 */
public class GeneralThreadService
{
    /** The thread pool. */
    private static final ListeningExecutorService SERVICE;

    /** A default callback that prints out any errors. */
    private static final FutureCallback<Result> DEFAULT_CALLBACK;

    /** Static initializer. */
    static
    {
        // setup the thread pool
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(Settings.NUMBER_OF_THREADS);
        SERVICE = MoreExecutors.listeningDecorator(fixedThreadPool);
        System.out.printf("GeneralThreadService started with %d threads.\n", Settings.NUMBER_OF_THREADS);
    
        // create a default callback
        DEFAULT_CALLBACK = new FutureCallback<Result>()
            {
                public void onSuccess(Result result)
                {
                }

                public void onFailure(Throwable t)
                {
                    System.out.println("=== EXECUTION EXCEPTION ===");
                    t.printStackTrace();
                }
            };        
    }

    /** Not instantiable. */
    private GeneralThreadService()
    {
        throw new IllegalArgumentException("not instantiable");
    }

    /** Run a job. */
    public static ListenableFuture<Result> submit(WorkUnit workUnit)
    {
        // add a default callback that prints out any error if one occurred
        ListenableFuture<Result> f = SERVICE.submit(workUnit);
        Futures.addCallback(f, DEFAULT_CALLBACK);
        return f;
    }

    /** Run a job with a callback. */
    public static ListenableFuture<Result> submit(WorkUnit workUnit, FutureCallback<Result> callback)
    {
        ListenableFuture<Result> f = SERVICE.submit(workUnit);
        Futures.addCallback(f, callback);
        return f;
    }

    /**
     * Run a bunch of jobs and wait for the result.
     * Treats both successes and failures as completed jobs.
     * @param workUnits the work to do
     * @param statusUpdate whether to return any status updates while waiting for the work to copmlete
     * @return all the results
     */
    public static List<Result> submitAndWait(List<WorkUnit> workUnits, boolean statusUpdate)
    {
        if ( workUnits == null || workUnits.size() == 0 )
            return ImmutableList.of();

        // create a latch so we can wait for all the jobs to finish
        int numberOfJobs = workUnits.size();
        final CountDownLatch latch = new CountDownLatch(numberOfJobs);

        // create a callback that decrements the latch when each job succeeds or fails
        FutureCallback<Result> callback = new FutureCallback<Result>()
            {
                public void onSuccess(Result result)
                {
                    latch.countDown();
                }

                public void onFailure(Throwable t)
                {
                    System.out.println("=== EXECUTION EXCEPTION ===");
                    t.printStackTrace();
                    latch.countDown();
                }
            };

        // submit the work
        List<ListenableFuture<Result>> futures = new ArrayList<>(numberOfJobs);
        for (WorkUnit u : workUnits)
            futures.add( GeneralThreadService.submit(u, callback) );
        
        // wait for the work to finish
        if ( statusUpdate )
            {
                while (true)
                    {
                        System.out.printf("%d of %d jobs complete   \r", numberOfJobs-latch.getCount(), numberOfJobs);
                        try
                            {
                                boolean done = latch.await(500, TimeUnit.MILLISECONDS);
                                if ( done )
                                    break;
                            }
                        catch (InterruptedException e) {}
                    }
                System.out.println("\nAll jobs complete.");
            }
        else
            {
                try { latch.await(); }
                catch ( Exception e ) { e.printStackTrace(); }
            }

        // return the results
        List<Result> results = new ArrayList<>(numberOfJobs);
        for (ListenableFuture<Result> f : futures)
            {
                try
                    {
                        Result result = f.get();
                        results.add(result);
                    }
                catch (Exception e)
                    {
                        System.out.println("Error retrieving result:");
                        e.printStackTrace();
                    }
            }
        return results;
    }

    /** Run a bunch of jobs and wait for the result with no status display. */
    public static List<Result> submitAndWaitSilently(List<WorkUnit> workUnits)
    {
        return submitAndWait(workUnits, false);
    }

    /** Run a bunch of jobs and wait for the result with a status display. */
    public static List<Result> submitAndWait(List<WorkUnit> workUnits)
    {
        return submitAndWait(workUnits, true);
    }

    /**
     * Run a remote job and automatically send back the result when it is finished.
     * @param workEnvelope the work to do
     * @param session where to send the result back to
     */
    public static void submit(final WorkEnvelope workEnvelope, final IoSession session)
    {
        // create a callback that sends back any successes and prints out any errors
        FutureCallback<Result> callback = new FutureCallback<Result>()
            {
                public void onSuccess(Result result)
                {
                    long serverID = workEnvelope.serverID;
                    ResultEnvelope resultEnvelope = new ResultEnvelope(result, null, Settings.HOSTNAME, serverID);
                    session.write(resultEnvelope);
                    System.out.printf("Sent back work unit %d.\n", serverID);
                }

                public void onFailure(Throwable t)
                {
                    System.out.println("=== EXECUTION EXCEPTION ===");
                    t.printStackTrace();
                    long serverID = workEnvelope.serverID;
                    ResultEnvelope resultEnvelope = new ResultEnvelope(null, t.getMessage(), Settings.HOSTNAME, serverID);
                    session.write(resultEnvelope);
                }
            };

        // submit the job
        ListenableFuture<Result> f = SERVICE.submit(workEnvelope.workUnit);
        Futures.addCallback(f, callback);
    }

    /** For testing. */
    public static void main(String[] args)
    {
        List<WorkUnit> workList = new ArrayList<>();
        for (int i=0; i < 16; i++)
            workList.add(new DummyWorkUnit(i==5)); // make unit 6 fail
        GeneralThreadService.submitAndWait(workList);
        System.exit(0);
    }
}
