 package com.ingres.util;
 
 import java.util.Queue;
 import java.util.concurrent.SynchronousQueue;
 
 public class QueuedThread extends Thread
 {
   private static Logging logger = new Logging(QueuedThread.class);
   private static int id = 0;
   private final Queue<QueuedThread> workerQueue;
   private final SynchronousQueue<Runnable> queue = new SynchronousQueue<Runnable>();
   
   public QueuedThread()
   {
     super(String.format("%1$s-%2$d", new Object[] { QueuedThread.class.getSimpleName(), Integer.valueOf(++id) }));
     setDaemon(true);
     this.workerQueue = null;
     start();
   }
   
   public QueuedThread(Queue<QueuedThread> queue)
   {
     super(String.format("%1$s-%2$d", new Object[] { QueuedThread.class.getSimpleName(), Integer.valueOf(++id) }));
     setDaemon(true);//设置为守护线程
     this.workerQueue = queue;
     start();//自启动 执行run函数
   }
   
   public boolean submitWork(Runnable runnable)
   {
     try
     {
       this.queue.put(runnable);
       logger.debug("submitWork successful: %1$s", new Object[] { runnable.toString() });
     }
     catch (InterruptedException ie)
     {
       return false;
     }
     return true;
   }
   
   public void run()
   {
     logger.debug("Running...", new Object[0]);
     for (;;)
     {
       if (this.workerQueue != null) {
         this.workerQueue.add(this);//当前守护线程添加到workerQueue队列
       }
       Runnable runnable = null;
       try
       {
         //等待任务
         logger.debug("Waiting for work...", new Object[0]);
         runnable = (Runnable)this.queue.take();
       }
       catch (InterruptedException ie)
       {
         break;
       }
       if (runnable != null)
       {
         logger.debug("Running work package %1$s...", new Object[] { runnable.toString() });
         try
         {
           //执行任务
           runnable.run();
           logger.debug("Finished work %1$s.", new Object[] { runnable.toString() });
         }
         catch (Throwable ex)
         {
           logger.debug("Unhandled exception", new Object[] { ex });
           Thread.UncaughtExceptionHandler eh = getUncaughtExceptionHandler();
           if (eh != null) {
             eh.uncaughtException(this, ex);
           }
         }
       }
     }
     logger.debug("Finished execution loop.", new Object[0]);
   }
 }
