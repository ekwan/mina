import java.util.*;
import java.io.*;
import com.google.common.collect.*;

/**
 * This singleton controls the behavior of the program.  Nothing trajectory-specific should be
 * stored here.  This is only for general program settings.
 */
public class Settings implements Immutable, Singleton
{
    // General Settings

        /** hostname (e.g., enj02.rc.fas.harvard.edu) */
        public static final String FULL_HOSTNAME;

        /** first field of hostname */
        public static final String HOSTNAME;

        /**
         * How many threads are available for various machines.  A case-insensitive substring match is used.
         * Map from hostnames to cores.  Priority is from insertion order.
         */
        public static final Map<String,Integer> NUMBER_OF_THREADS_MAP;

        /**
         * If the remote host is not in the database, how much work should it do at one time?
         */
        public static final int DEFAULT_REMOTE_NUMBER_OF_THREADS = 4;

        /** number of available threads for this machine */
        public static final int NUMBER_OF_THREADS;

        /** current working directory */
        public static final String WORKING_DIRECTORY;

        /** the main class name */
        public static final String MAIN_CLASS;

    // Gaussian Job Parameters
        
        /** Where the g09 script is. */
        public static final String GAUSSIAN_JOB_DIRECTORY;
    
        /** How many filenames are available for Gaussian jobs.  */
        public static final int GAUSSIAN_JOB_MAX_FILENAMES = 20000;

        /** Where the scratch directory is for various machines.  Can be an environment variable.  Map from hostnames to variables. */
        public static final Map<String,String> GAUSSIAN_SCRATCH_DIRECTORY_MAP;

        /** Where the scratch directory is for this machine. */
        public static final String GAUSSIAN_SCRATCH_DIRECTORY;

    // Network Settings

        /** If a client runs, where should it look for the server? */
        public static final String SERVER_HOSTNAME = "dae22.rc.fas.harvard.edu";
        //public static final String SERVER_HOSTNAME = "127.0.0.1"; // use for localhost

        /** The server will listen for connections on this port. */
        public static final int LISTENING_PORT = 9876;

        /** The client will try to connect to the server this number of times. */
        public static final int MAX_CONNECTION_ATTEMPTS = 5;

        /** How long to wait between connection attempts in seconds. */
        public static final int CONNECTION_RETRY_DELAY = 5;

    /** static initializer */
    static
    {
        String temp = "";

        // set hostname
        try
            {
                temp = java.net.InetAddress.getLocalHost().getHostName();
            }
        catch (Exception e)
            {
                System.out.println("Warning, unable to detect hostname.  Using localhost.");
                temp = "localhost";
            }
        FULL_HOSTNAME = temp;

        if ( FULL_HOSTNAME.length() > 0 )
            temp = FULL_HOSTNAME.split("\\.")[0];
        else
            temp = "";
        HOSTNAME = temp;
        String hostname = HOSTNAME.toLowerCase();

        // set number of threads
        Map<String,Integer> tempMap = new LinkedHashMap<>();
        tempMap.put("enj", 12);
        tempMap.put("dae", 8);
        tempMap.put("holy", 64);
        tempMap.put("sh", 16);
        NUMBER_OF_THREADS_MAP = ImmutableMap.copyOf(tempMap);

        // will default to this if we can't find what we're looking for
        int tempThreads = Runtime.getRuntime().availableProcessors();
        boolean threadBoolean = false;
        for (String thisHostName : tempMap.keySet())
            {
                if (hostname.contains(thisHostName))
                    {
                        int thisNumberOfThreads = tempMap.get(thisHostName);
                        System.out.printf("Using %d threads for current host (%s).\n", thisNumberOfThreads, HOSTNAME);
                        tempThreads = thisNumberOfThreads;
                        threadBoolean = true;
                        break;
                    }
            }
        if ( !threadBoolean )
            System.out.printf("Defaulting to %d threads for current host (%s).\n", tempThreads, HOSTNAME);
        NUMBER_OF_THREADS = tempThreads;
        
        // get working directory
        temp = System.getProperty("user.dir") + "/";
        WORKING_DIRECTORY = temp;
 
        // set the main class name
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement main = stack[stack.length - 1];
        MAIN_CLASS = main.getClassName();

        // for Gaussian jobs
        GAUSSIAN_JOB_DIRECTORY = WORKING_DIRECTORY + "g09/";
        
        Map<String,String> tempMap2 = new LinkedHashMap<>();
        tempMap2.put("enj",  "/scratch/");
        tempMap2.put("dae",  "/scratch/");
        tempMap2.put("holy", "/scratch/");
        tempMap2.put("sh",   "$LOCAL_SCRATCH");  // note the lack of a trailing slash
        GAUSSIAN_SCRATCH_DIRECTORY_MAP = ImmutableMap.copyOf(tempMap2);

        // will default to this if we can't find what we're looking for
        String tempScratch = WORKING_DIRECTORY + "g09/";
        boolean scratchBoolean = false;
        for (String thisHostName : tempMap2.keySet())
            {
                if (hostname.contains(thisHostName))
                    {
                        String thisScratch = tempMap2.get(thisHostName);
                        System.out.printf("Using %s as scratch for current host (%s).\n", thisScratch, HOSTNAME);
                        tempScratch = thisScratch;
                        scratchBoolean = true;
                        break;
                    }
            }
        if ( !scratchBoolean )
            System.out.printf("Defaulting to scratch in %s for current host (%s).\n", tempScratch, HOSTNAME);
        
        // attempt to expand scratch directory if necessary
        if ( tempScratch.contains("$") )
            {
                try
                    {
                        String tempScratch2 = System.getenv(tempScratch.replace("$",""));
                        if ( tempScratch2 == null )
                            throw new NullPointerException("no match found");
                        System.out.printf("Expanded scratch directory %s to %s.\n", tempScratch, tempScratch2);
                        tempScratch = tempScratch2 + "/";
                     }
                catch (Exception e)
                    {
                        System.out.println("Unable to expand environment variable: " + tempScratch);
                        e.printStackTrace();
                        System.exit(1);
                    }
            }
        
        GAUSSIAN_SCRATCH_DIRECTORY = tempScratch;
    }

    /** not instantiable */
    private Settings()
    {
    }

    /**
     * Determines how many jobs this host should be running simultaneously.
     * @param hostname the host to get the number of threads for
     * @return how many jobs this host should run simultaneously
     */
    public static int getNumberOfThreads(String hostname)
    {
        int numberOfThreads = DEFAULT_REMOTE_NUMBER_OF_THREADS;
        for (String host : NUMBER_OF_THREADS_MAP.keySet())
            {
                if (hostname.contains(host))
                    return NUMBER_OF_THREADS_MAP.get(host);
            }
        return numberOfThreads;
    }

    /** for testing */
    public static void main(String[] args)
    {
    }
}
