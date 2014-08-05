 package com.ingres.util;
 
 import java.io.PrintStream;
 
 public class LogStream
   extends PrintStream
 {
   private int logLevel = 4;
   private StringBuffer buf = null;
   
   public LogStream(int level)
   {
     super(System.out);
     this.logLevel = level;
   }
   
   public LogStream()
   {
     super(System.out);
   }
   
   public void print(String s)
   {
     if (this.buf == null) {
       this.buf = new StringBuffer(s);
     } else {
       this.buf.append(s);
     }
   }
   
   public void println(String s)
   {
     if (this.buf == null)
     {
       Logging.Log(this.logLevel, s, new Object[0]);
     }
     else
     {
       this.buf.append(s);
       Logging.Log(this.logLevel, this.buf.toString(), new Object[0]);
       this.buf = null;
     }
   }
   
   public void println()
   {
     if (this.buf != null)
     {
       Logging.Log(this.logLevel, this.buf.toString(), new Object[0]);
       this.buf = null;
     }
   }
 }
