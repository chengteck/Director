 package com.ingres.util;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.AbstractExecutorService;
 import java.util.concurrent.TimeUnit;
 
 public class QueuedExecutorService
   extends AbstractExecutorService
 {
   private boolean shutdown = false;
   private final QueuedThread queuedThread;
   
   public QueuedExecutorService(QueuedThread thread)
   {
     this.queuedThread = thread;
   }
   
   public boolean awaitTermination(long timeout, TimeUnit unit)
     throws InterruptedException
   {
     return true;
   }
   
   public void shutdown()
   {
     shutdownNow();
   }
   
   public List<Runnable> shutdownNow()
   {
     this.shutdown = true;
     return new ArrayList<Runnable>();
   }
   
   public boolean isShutdown()
   {
     return this.shutdown;
   }
   
   public boolean isTerminated()
   {
     return isShutdown();
   }
   
   public void execute(Runnable command)
   {
     this.queuedThread.submitWork(command);
   }
 }

