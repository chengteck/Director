 package com.ingres.util;
 
 import java.io.IOException;
 import java.io.InputStream;
 
 public class CountedInputStream
   extends InputStream
 {
   private ClientStatistics statObj = null;
   private InputStream is = null;
   
   public CountedInputStream(InputStream is, ClientStatistics statObj)
   {
     this.is = is;
     this.statObj = statObj;
   }
   
   public int available()
     throws IOException
   {
     return this.is.available();
   }
   
   public void close()
     throws IOException
   {
     this.is.close();
   }
   
   public long skip(long n)
     throws IOException
   {
     long num = this.is.skip(n);
     this.statObj.addToBytes(num, true);
     return num;
   }
   
   public int read()
     throws IOException
   {
     int i = this.is.read();
     this.statObj.addToBytes(1L, true);
     return i;
   }
   
   public int read(byte[] b)
     throws IOException
   {
     int num = this.is.read(b);
     if (num != -1) {
       this.statObj.addToBytes(num, true);
     }
     return num;
   }
   
   public int read(byte[] b, int off, int len)
     throws IOException
   {
     int num = this.is.read(b, off, len);
     if (num != -1) {
       this.statObj.addToBytes(num, true);
     }
     return num;
   }
 }
