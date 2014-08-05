 package com.ingres.util;
 
 import java.io.IOException;
 import java.io.OutputStream;
 
 public class CountedOutputStream
   extends OutputStream
 {
   private ClientStatistics statObj = null;
   private OutputStream os = null;
   
   public CountedOutputStream(OutputStream os, ClientStatistics statObj)
   {
     this.os = os;
     this.statObj = statObj;
   }
   
   public void close()
     throws IOException
   {
     this.os.close();
   }
   
   public void flush()
     throws IOException
   {
     this.os.flush();
   }
   
   public void write(int b)
     throws IOException
   {
     this.os.write(b);
     this.statObj.addToBytes(1L, false);
   }
   
   public void write(byte[] b)
     throws IOException
   {
     this.os.write(b);
     this.statObj.addToBytes(b.length, false);
   }
   
   public void write(byte[] b, int off, int len)
     throws IOException
   {
     this.os.write(b, off, len);
     this.statObj.addToBytes(len, false);
   }
 }
