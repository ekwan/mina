import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.common.collect.*;
import java.util.concurrent.atomic.*;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.future.WriteFuture;

/**
 * Keeps track of which work units are currently running on other machines.
 * Designed to work with Server.
 */
public class WorkUnitDatabase implements Singleton
{
    /** Maps IDs to work units and the threads that are running them. */
    private static final Map<Long,DatabaseEntry> MAP;

    /** Synchronizes the parallel lists. */
    private static final Object INTERNAL_LOCK;

    /** The work that is to be done. */
    private static final LinkedList<WorkEnvelope> QUEUE; 

    /** Standard constructor. */
    public WorkUnitDatabase()
    {
        throw new IllegalArgumentException("not instantiable");
    }

    /** Static initializer. */
    static
    {
        MAP = new HashMap<>();
        INTERNAL_LOCK = new Object();
        QUEUE = new LinkedList<>();
        KNOWN_CLIENTS = new ArrayList<>();
    }

    /** Retrieve an entry from the map.  Throws an exception if it is not found. */
    private static DatabaseEntry get(long serverID)
    {
        synchronized (INTERNAL_LOCK)
            {
                if ( !MAP.containsKey(serverID) )
                    throw new IllegalArgumentException("expected to find an entry for serverID " + serverID);
                return MAP.get(serverID);
            }
    }

    /**
     * Update the map that keeps track of everything.
     * @param newEntry this entry will replace the old entry
     */
    private static void update(DatabaseEntry newEntry)
    {
        synchronized (INTERNAL_LOCK)
        {
            if ( !MAP.containsKey(newEntry.serverID) )
                throw new IllegalArgumentException("serverID should already be in database but was not found, cannot update");
            MAP.put(newEntry.serverID, newEntry);
        }
     }

    /** Submit a job to the queue. */
    public static void submit(WorkEnvelope workEnvelope)
    {
        DatabaseEntry entry = new DatabaseEntry(workEnvelope.serverID, null, Status.SUBMITTED, null, null);
        synchronized (INTERNAL_LOCK)
        {
            if ( MAP.containsKey(workEnvelope.serverID) )
                throw new IllegalArgumentException("serverID already in database, cannot create new key");
            QUEUE.add(workEnvelope);
            MAP.put(workEnvelope.serverID, entry);
        }
    }

    /** Take a job out of the queue and send it to a client. */
    public static void sendOutWork(String remoteHostname, IoSession session)
    {
        synchronized (INTERNAL_LOCK)
        {
            // return if there is no work to send out
            if ( QUEUE.size() == 0 )
                return;

            // check how many jobs are running on this client
            int alreadyRunning = 0;
            for (Long serverID : MAP.keySet())
                {
                    DatabaseEntry entry = MAP.get(serverID);
                    if ( entry.status == Status.SENT_OUT && entry.hostname.equals(remoteHostname) )
                        alreadyRunning++;
                }

            // check if this client is full
            int remoteThreads = Settings.getNumberOfThreads(remoteHostname);
            if ( alreadyRunning >= remoteThreads )
                return;

            // get one piece of work and check the consistency of the database
            WorkEnvelope envelope = QUEUE.poll();
            if ( !MAP.containsKey(envelope.serverID)  )
                throw new IllegalArgumentException("database is in an inconsistent state");
            DatabaseEntry oldEntry = MAP.get(envelope.serverID);
            if ( oldEntry.status != Status.SUBMITTED )
                throw new IllegalArgumentException("expected status to be SUBMITTED to change it to SENT_OUT");
            
            // send out the work
            try
                {
                    WriteFuture future = session.write(envelope);
                    System.out.printf("Sent out work unit ID %d to %s.\n", envelope.serverID, remoteHostname);

                    // update database
                    DatabaseEntry entry = new DatabaseEntry(envelope.serverID, remoteHostname, Status.SENT_OUT, null, null);
                }
            catch (Exception e)
                {
                    // print out the problem
                    System.out.printf("Problem sending work unit ID %d:\n", envelope.serverID);
                    e.printStackTrace();

                    // return the job to the queue
                    QUEUE.add(envelope);
                }

        }
    }

    /** Receive a work unit. */
    public static ResultEnvelope receive(ResultEnvelope resultEnvelope)
    {
    }

    /** Mark a job as having finished succesfully. */
    public static void completed(long serverID, Result result)
    {
        DatabaseEntry oldEntry = get(serverID);
        if ( oldEntry.status != Status.SENT_OUT )
            throw new IllegalArgumentException("expected status to be SENT_OUT to change the status to COMPLETED");
        DatabaseEntry newEntry = new DatabaseEntry(serverID, oldEntry.hostname, Status.COMPLETED, result, null);
        update(newEntry);
    }

    /** Mark a job as having failed. */
    public static void failed(long serverID, String errorMessage)
    {
        DatabaseEntry oldEntry = get(serverID);
        if ( oldEntry.status != Status.SENT_OUT )
            throw new IllegalArgumentException("expected status to be SENT_OUT to change the status to FAILED");
        DatabaseEntry newEntry = new DatabaseEntry(serverID, oldEntry.hostname, Status.FAILED, null, errorMessage);
        update(newEntry);
    }

    /** Mark all the work that has been dispatched to the specified host as dead. */
    public static void markAsDead(String remoteHostname)
    {
        DatabaseEntry oldEntry = get(serverID);
        if ( oldEntry.status != Status.FAILED )
            throw new IllegalArgumentException("expected status to be FAILED to change the status to SUBMITTED");
        DatabaseEntry newEntry = new DatabaseEntry(serverID, null, Status.SUBMITTED, null, null);
        update(newEntry);
    }

    /** Forget about all jobs that have completed successfully or failed to save memory. */
    public static void purge()
    {
        synchronized (INTERNAL_LOCK)
            {
                List<Long> toBePurged = new ArrayList<>();
                for (Long serverID : MAP.keySet())
                    {
                        DatabaseEntry entry = MAP.get(serverID);
                        if ( entry.status == Status.COMPLETED || entry.status == Status.FAILED )
                            toBePurged.add(serverID);
                    }
                for (Long serverID : toBePurged)
                    MAP.remove(serverID);
            }
    }

    /** Represents the state of each job. */
    public enum Status
    {
        /** The job has been submitted to the queue, but has not been sent out yet. */
        SUBMITTED,

        /** The job has been transmitted over the network. */
        SENT_OUT,

        /** The job completed successfully. */
        COMPLETED,

        /** Something went wrong. */
        FAILED;
    }

    /** Keeps track of each piece of work. */
    private static class DatabaseEntry implements Immutable
    {
        /** The ID of the WorkEnvelope associated with this piece of work. */
        private final long serverID;

        /** Where the work is presently or was done. */
        private final String hostname;

        /** The current status of the job. */
        private final Status status;

        /** The result of the computation. */
        private final Result result;

        /** The error message obtained. */
        private final String errorMessage;

        public DatabaseEntry(long serverID, String hostname, Status status, Result result, String errorMessage)
        {
            this.serverID = serverID;
            this.hostname = hostname;
            this.status = status;
            this.result = result;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString()
        {
            return String.format("DatabaseEntry for serverID %d (%s, %s)", serverID, hostname, status.toString());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(serverID, hostname, status, result, errorMessage);
        }

        @Override
        public boolean equals(Object obj)
        {
            if ( obj == null )
                return false;
            if ( obj == this ) 
                return true;
            if ( !(obj instanceof DatabaseEntry) )
                return false;

            DatabaseEntry d = (DatabaseEntry)obj;
            if ( Objects.equals(serverID, d.serverID) &&
                 Objects.equals(hostname, d.hostname) &&
                 Objects.equals(status, d.status) &&
                 Objects.equals(result, d.result) &&
                 Objects.equals(errorMessage, d.errorMessage) )
                return true;
            return false;
        }
    }
}
