 package com.ingres.server;
 
 import com.ingres.FileHandle;
 import com.ingres.IIapi;
 import com.ingres.IIapi.Exception;
 import com.ingres.RemoteIIapi;
 //import com.ingres.object.InstanceObject;
 import com.ingres.util.Logging;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.Reader;
 import java.nio.ByteBuffer;
 import java.nio.CharBuffer;
 import java.nio.charset.Charset;
 import java.nio.charset.CharsetDecoder;
 import java.nio.charset.CoderResult;
 import java.nio.charset.CodingErrorAction;
 
 public class RemoteFileReader
   extends Reader
 {
   private static Charset asciiCharset = Charset.forName("US-ASCII");
   private String name;
   private IIapi inst;
   private CharsetDecoder decoder;
   private FileHandle handle = null;
   private InputStream inputStream = null;
   private RemoteConnection conn = null;
   public boolean isRemote = false;
   private boolean isRemoteStream = false;
   private long maxFileSize = 0L;
   public long size = -1L;
   public long pos = 0L;
   public long bytesRead = 0L;
   public boolean limited = false;
   
   public RemoteFileReader(IIapi inst, String name)
     throws FileNotFoundException
   {
     this(inst, name, null, 0L, false);
   }
   
   public RemoteFileReader(IIapi inst, String name, long maxFileSize, boolean headOrTail)
     throws FileNotFoundException
   {
     this(inst, name, null, maxFileSize, headOrTail);
   }
   
   public RemoteFileReader(IIapi inst, String name, Charset charset, long maxFileSize, boolean headOrTail)
     throws FileNotFoundException
   {
     this.name = name;
     //this.inst = instObj.getInst();
     this.inst = inst;
     try
     {
       if ((this.isRemote = this.inst.isRemote()))
       {
         RemoteIIapi remoteInst = (RemoteIIapi)this.inst;
         this.conn = remoteInst.getRemoteConnection();
         int protoLevel = this.conn.getProtocolLevel();
         if (protoLevel >= 5)
         {
           this.inputStream = this.conn.getRemoteFileInputStream(name);
           this.size = this.conn.getRemoteFileSize();
           this.isRemoteStream = true;
         }
         else if (protoLevel >= 2)
         {
           this.handle = this.inst.FSopenFile(name);
           if (this.handle == null) {
             throw new FileNotFoundException(name);
           }
         }
         else
         {
           throw new FileNotFoundException(name);
         }
       }
       else
       {
         this.inputStream = new FileInputStream(name);
         try
         {
           this.size = this.inputStream.available();
         }
         catch (IOException ignore) {}
       }
     }
     catch (IIapi.Exception ie)
     {
       throw new FileNotFoundException(name);
     }
     if ((this.handle == null) && (this.inputStream == null)) {
       throw new FileNotFoundException(name);
     }
     this.maxFileSize = maxFileSize;
     if ((this.size != -1L) && (maxFileSize != 0L) && (headOrTail))
     {
       long skip = Math.max(0L, this.size - maxFileSize);
       if (skip > 0L)
       {
         if (this.inputStream != null) {
           try
           {
             Logging.Debug("skip %1$d bytes of log file (out of %2$d)", new Object[] { Long.valueOf(skip), Long.valueOf(this.size) });
             this.inputStream.skip(skip);
           }
           catch (IOException ignore) {}
         }
         this.pos = skip;
         this.limited = true;
       }
     }
     Charset cs = charset == null ? asciiCharset : charset;
     this.decoder = cs.newDecoder();
     this.decoder.onMalformedInput(CodingErrorAction.REPLACE);
     this.decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
     this.decoder.replaceWith("●");
   }
   
   public int read(char[] buf, int off, int len)
     throws IOException
   {
     if ((this.maxFileSize != 0L) && (this.bytesRead > this.maxFileSize))
     {
       this.limited = true;
       return -1;
     }
     int rlen = 0;
     byte[] bbuf = new byte[len];
     ByteBuffer bb = ByteBuffer.wrap(bbuf);
     CharBuffer cb = CharBuffer.wrap(buf, off, len);
     if (this.inputStream != null) {
       rlen = this.inputStream.read(bbuf, 0, len);
     } else if (this.handle != null) {
       rlen = this.inst.FSreadFile(this.handle, this.pos, bbuf);
     }
     CoderResult result;
     if (rlen > 0)
     {
       this.pos += rlen;
       if ((this.maxFileSize != 0L) && (this.bytesRead + rlen > this.maxFileSize))
       {
         rlen = (int)(this.maxFileSize - this.bytesRead);
         this.limited = true;
       }
       this.bytesRead += rlen;
       if (rlen < len) {
         bb.limit(rlen);
       }
       result = this.decoder.decode(bb, cb, false);
     }
     return rlen;
   }
   
   public void close()
     throws IOException
   {
     if (this.inputStream != null) {
       try
       {
         if ((this.isRemoteStream) && (this.conn != null)) {
           this.conn.releaseRemoteFileStream();
         } else {
           this.inputStream.close();
         }
       }
       finally
       {
         this.inputStream = null;
       }
     } else if (this.handle != null) {
       try
       {
         this.inst.FScloseFile(this.handle);
       }
       finally
       {
         this.handle = null;
       }
     }
   }
 }
