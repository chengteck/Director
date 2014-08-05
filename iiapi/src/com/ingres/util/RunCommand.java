 package com.ingres.util;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.PrintStream;
 
 public class RunCommand
 {
   private int errorLevel = -2147483648;
   private ProcessBuilder pb = null;
   private Process p = null;
   private BufferedReader stdInput = null;
   private PrintStream out = System.out;
   
   public int getErrorLevel()
   {
     if (this.errorLevel == -2147483648) {
       try
       {
         this.errorLevel = this.p.waitFor();
       }
       catch (InterruptedException ie)
       {
         Logging.Except(ie);
       }
     }
     return this.errorLevel;
   }
   
   public void setEchoStream(PrintStream o)
   {
     this.out = o;
   }
   
   public RunCommand(String... command)
   {
     this.pb = new ProcessBuilder(command);
     this.pb.redirectErrorStream(true);
   }
   
   public BufferedReader run()
   {
     try
     {
       if (Logging.isDebugEnabled())
       {
         StringBuilder sb = new StringBuilder("RunCommand.run: ");
         for (String s : this.pb.command()) {
           sb.append(s).append(' ');
         }
         Logging.Debug(sb.toString(), new Object[0]);
       }
       this.p = this.pb.start();
       
       this.stdInput = new BufferedReader(new InputStreamReader(this.p.getInputStream()));
       
 
       return this.stdInput;
     }
     catch (IOException ioe)
     {
       Logging.Except(ioe);
     }
     return null;
   }
   
   public BufferedReader run(File workingDir)
   {
     this.pb.directory(workingDir);
     return run();
   }
   
   public int runToCompletion(boolean echoOutput)
   {
     BufferedReader output = run();
     String line = null;
     try
     {
       while ((line = output.readLine()) != null) {
         if (echoOutput) {
           this.out.println(line);
         }
       }
       output.close();
       this.errorLevel = this.p.waitFor();
     }
     catch (IOException ioe)
     {
       Logging.Except(ioe);
     }
     catch (InterruptedException ie)
     {
       Logging.Except(ie);
     }
     return this.errorLevel;
   }
   
   public int runToCompletion()
   {
     return runToCompletion(true);
   }
   
   public int runToCompletion(PrintStream o)
   {
     setEchoStream(o);
     return runToCompletion(true);
   }
   
   public int runToCompletion(File workingDir)
   {
     this.pb.directory(workingDir);
     return runToCompletion();
   }
   
   public int runToCompletion(File workingDir, PrintStream o)
   {
     this.pb.directory(workingDir);
     setEchoStream(o);
     return runToCompletion();
   }
   
   public int runToCompletion(File workingDir, boolean echoOutput)
   {
     this.pb.directory(workingDir);
     return runToCompletion(echoOutput);
   }
 }
