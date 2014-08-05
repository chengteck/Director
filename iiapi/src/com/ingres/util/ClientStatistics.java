 package com.ingres.util;
 
 import java.io.PrintStream;
 import java.util.HashMap;
 import java.util.Map;
 
 public class ClientStatistics
   implements Runnable
 {
   public static final Map<String, ClientStatistics> runningClients = new HashMap<String, ClientStatistics>();
   public static long numberClients = 0L;
   public static int maxConcurrency = 0;
   public static long totalNumberBytes = 0L;
   public static long totalNumberBytesRead = 0L;
   public static long minNumberBytes = 9223372036854775807L;
   public static long maxNumberBytes = -9223372036854775808L;
   public static long totalTime = 0L;
   public static long minTime = 9223372036854775807L;
   public static long maxTime = -9223372036854775808L;
   public static long lastRequestTime = Environment.highResTimer();
   public static long reportInterval = 0L;
   private static LogStream strm = null;
   private static final String RUNNING_CLIENTS_TITLE = "============== Running Clients ==============";
   private static final String RUNNING_CLIENTS_UNDER = "=============================================";
   private static final String CLIENT_STATISTICS_TITLE = "============= Client Statistics ========================";
   private static final String CLIENT_STATISTICS_UNDER = "========================================================";
   private static final String SESSION_STATISTICS_TITLE = "============== Session Statistics ==============";
   private static final String SESSION_STATISTICS_UNDER = "================================================";
   private static final int SCALE = Environment.highResTimeScaleFactor();
   private String clientName = null;
   private Object client = null;
   private long numBytes = 0L;
   private long numBytesRead = 0L;
   private long startTime = 0L;
   
   private ClientStatistics(String name, Object client)
   {
     this.clientName = name;
     this.client = client;
     this.startTime = Environment.highResTimer();
   }
   
   public static ClientStatistics registerClient(String name, Object client)
   {
     ClientStatistics clientObj = new ClientStatistics(name, client);
     synchronized (runningClients)
     {
       runningClients.put(name, clientObj);
       maxConcurrency = Math.max(maxConcurrency, runningClients.size());
       numberClients += 1L;
     }
     return clientObj;
   }
   
   public void unregisterClient()
   {
     synchronized (runningClients)
     {
       runningClients.remove(this.clientName);
       
 
       long endTime = Environment.highResTimer();
       long sessionTime = endTime - this.startTime;
       totalTime += sessionTime;
       lastRequestTime = endTime;
       minTime = Math.min(minTime, sessionTime);
       maxTime = Math.max(maxTime, sessionTime);
       totalNumberBytes += this.numBytes;
       totalNumberBytesRead += this.numBytesRead;
       minNumberBytes = Math.min(minNumberBytes, this.numBytes);
       maxNumberBytes = Math.max(maxNumberBytes, this.numBytes);
       if (Logging.isLevelEnabled(4))
       {
         Logging.Info("============== Session Statistics ==============", new Object[0]);
         Logging.Info("                  Session name: %1$s", new Object[] { this.clientName });
         Logging.Info("          Number of bytes read: %1$s", new Object[] { Num.fmt1(this.numBytesRead, 16) });
         Logging.Info("       Number of bytes written: %1$s", new Object[] { Num.fmt1(this.numBytes - this.numBytesRead, 16) });
         Logging.Info("Total number bytes transferred: %1$s", new Object[] { Num.fmt1(this.numBytes, 16) });
         Logging.Info(" Total time spent this session: %1$s", new Object[] { Num.fmt2(sessionTime, SCALE, 12) });
         Logging.Info("================================================", new Object[0]);
       }
     }
     this.clientName = null;
     this.client = null;
   }
   
   public static int currentClientCount()
   {
     synchronized (runningClients)
     {
       return runningClients.size();
     }
   }
   
   public void addToBytes(long num, boolean read)
   {
     this.numBytes += num;
     if (read) {
       this.numBytesRead += num;
     }
   }
   
   public static void dumpRunningClients(PrintStream out)
   {
     out.println("============== Running Clients ==============");
     synchronized (runningClients)
     {
       for (ClientStatistics client : runningClients.values()) {
         out.println(client.client.toString());
       }
     }
     out.println("=============================================");
   }
   
   public static void dumpStatistics(PrintStream out)
   {
     out.println("============= Client Statistics ========================");
     synchronized (runningClients)
     {
       out.print("Total number of client sessions so far: ");out.println(Num.fmt1(numberClients, 16));
       out.print("             Current number of clients: ");out.println(Num.fmt1(runningClients.size(), 16));
       out.print("  Maximum number of concurrent clients: ");out.println(Num.fmt1(maxConcurrency, 16));
       out.print("                  Number of bytes read: ");out.println(Num.fmt1(totalNumberBytesRead, 16));
       out.print("               Number of bytes written: ");out.println(Num.fmt1(totalNumberBytes - totalNumberBytesRead, 16));
       out.print("     Total number of bytes transferred: ");out.println(Num.fmt1(totalNumberBytes, 16));
       if (numberClients != 0L)
       {
         out.print("  Minimum number of bytes in a session: ");out.println(Num.fmt1(minNumberBytes, 16));
         out.print("  Maximum number of bytes in a session: ");out.println(Num.fmt1(maxNumberBytes, 16));
         long average = totalNumberBytes / numberClients;
         out.print("   Average number of bytes per session: ");out.println(Num.fmt1(average, 16));
       }
       out.print("     Total time (sec) for all sessions: ");out.println(Num.fmt2(totalTime, SCALE, 12));
       long elapsed = Environment.highResTimer() - lastRequestTime;
       if (numberClients != 0L)
       {
         out.print("             Minimum time in a session: ");out.println(Num.fmt2(minTime, SCALE, 12));
         out.print("             Maximum time in a session: ");out.println(Num.fmt2(maxTime, SCALE, 12));
         double avg = totalTime / numberClients;
         out.print("   Average amount of time in a session: ");out.println(Num.fmt2(avg, SCALE, 12));
         out.print(" Time (secs) since last client request: ");
       }
       else
       {
         out.print("             Time (secs) since startup: ");
       }
       out.println(Num.fmt2(elapsed, SCALE, 12));
     }
     out.println("========================================================");
   }
   
   public void run()
   {
     try
     {
       for (;;)
       {
         Thread.sleep(reportInterval);
         dumpStatistics(strm);
       }
     }
     catch (InterruptedException ie)
     {
       Logging.Info("%1$s thread interrupted, now exiting.", new Object[] { Thread.currentThread().getName() });
     }
   }
   
   public static void startMonitorThread(int minutes)
   {
     Logging.Debug("ClientStatistics.startMonitorThread", new Object[0]);
     ClientStatistics cs = new ClientStatistics("Monitor Thread", null);
     Thread backgroundThread = new Thread(cs, "Monitor");
     backgroundThread.setDaemon(true);
     if (minutes <= 0) {
       minutes = 30;
     }
     reportInterval = minutes * 60L * 1000L;
     strm = new LogStream();
     backgroundThread.start();
   }
 }
