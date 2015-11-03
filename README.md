This implements a distributed computing paradigm using the Apache Mina library and
Guava FutureCallbacks.  You can start a Server that dispatches work to multiple
remote Clients over TCP.  You can specify how many work units each Client should
work on in parallel at one time.  If a Client dies, the work is automatically
re-queued.  This implementation uses non-blocking I/O and a thread pool to maximize
performance for many connections.

I have included a test which is 100 DummyWorkUnits.  These are just jobs that wait
a few seconds before stopping.  Job number 6 is configured to throw an exception
when run.  To start the server, run the compile script.  This will compile all the
classes and start the server.  Then, use the compile2 script to start the clients
in the same directory on other machines.  (Alternatively, you could have different
copies of the repo in different locations.)

It is necessary to set the hostname of the Server in Settings.SERVER_HOSTNAME.  This
lets the Clients find the Server.  The number of threads per client can be set in
Server.NUMBER_OF_THREADS_MAP.  Obviously, none of this will work if there are any
blocked or firewalled ports.

Note that there is only one IoHandlerAdapter for the Server (and one for the Client).
Hostnames are stored with key,value pairs using IoSession.setAttribute and
IoSession.getAttribute.

-Eugene Kwan
