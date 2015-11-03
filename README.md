This implements a distributed computing paradigm using the Apache Mina library and
Guava FutureCallbacks.  You can start a Server that dispatches work to multiple
remote Clients over TCP.  You can specify how many work units each Client should
work on in parallel at one time.  If a Client dies, the work is automatically
re-queued.  This implementation uses non-blocking I/O and a thread pool to maximize
performance for many connections.

-Eugene Kwan
