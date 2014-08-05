 package com.ingres.server;
 
 import com.ingres.ConnectionInfo;
import com.ingres.IIapi.Exception;
import com.ingres.IIapi;
import com.ingres.IIapi.SysInfo;
import com.ingres.SessionAuth;
import com.ingres.discovery.*;
import com.ingres.discovery.Discovery.Info;
import com.ingres.util.Intl;
import com.ingres.util.Logging;
import com.ingres.util.NetworkUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
 
 /**
  * 远程连接
  * @author Ingres
  * @modify zxbing
  */
 public class RemoteConnection
 {
   private Socket sock = null;
   private boolean triedConnect = false;
   private ObjectInputStream is = null;
   private ObjectOutputStream os = null;
   private ReentrantLock lock = null;
   private static int supProtoLvl = 5;
   private int curProtoLvl = 1;
   private static int supProtoMinorLvl = 101;
   private int curProtoMinorLvl = -1;
   private int instID = 0;
   private boolean commandChannel = false;
   public static final int CONNECT_TIMEOUT = 5000; //连接超时
   public static final int READ_TIMEOUT = 0;
   private ConnectionInfo ci = null;
   private Discovery.Info dInfo = null;
   private SessionAuth sessionAuth = null;
   private RemoteSecurity security = null;
   private boolean sessionActive = false;
   private long remoteFileSize = -1L;
   private static Logging logger = new Logging(RemoteConnection.class);
   
   public RemoteConnection(ConnectionInfo ci)
   {
     this.ci = ci;
     this.dInfo = ci.getDiscoveryInfo();
     this.sessionAuth = ci.getSessionAuth();
   }
   
   public ObjectInputStream getInputStream()
   {
     return this.is;
   }
   
   public ObjectOutputStream getOutputStream()
   {
     return this.os;
   }
   
   public int getProtocolLevel()
   {
     return this.curProtoLvl;
   }
   
   public int getInstallationID()
   {
     return this.instID;
   }
   
   public Discovery.Info getDiscoveryInfo()
   {
     return this.dInfo;
   }
   
   public boolean isLocal()
   {
     return this.ci.getLocal();
   }
   
   public boolean isConnected()
   {
     return (this.sock != null) && (this.sock.isConnected());
   }
   
   private Socket getConnected(String hostName, int remotePort)
     throws IOException
   {
     logger.info("Connecting to %1$s:%2$d...", new Object[] { hostName, Integer.valueOf(remotePort) });
     return NetworkUtil.connect(hostName, remotePort, 5000, 0);
   }
   
   private Socket getConnected(InetAddress hostAddr, int remotePort)
     throws IOException
   {
     logger.info("Connecting to %1$s:%2$d...", new Object[] { hostAddr.toString(), Integer.valueOf(remotePort) });
     return NetworkUtil.connect(hostAddr, remotePort, 5000, 0);
   }
   
   public boolean reconnect()
     throws IOException, IIapi.Exception
   {
     return connect(this.commandChannel ? RemoteCommand.COMMAND_SIGNATURE : RemoteCommand.SIGNATURE);
   }
   
   public boolean connect(byte[] signature)
     throws IOException, IIapi.Exception
   {
     if (this.dInfo == null) {
       throw new IllegalStateException(Intl.getString(RemoteCommand.pkg, "connect.noHostInfo"));
     }
     if (this.sock != null) {
       throw new IllegalStateException(Intl.getString(RemoteCommand.pkg, "connect.noMultipleConnect"));
     }
     if (Arrays.equals(RemoteCommand.COMMAND_SIGNATURE, signature)) {
       this.commandChannel = true;
     } else if (!Arrays.equals(RemoteCommand.SIGNATURE, signature)) {
       throw new IllegalStateException(Intl.formatString(RemoteCommand.pkg, "connect.wrongSignature", new Object[] { Arrays.toString(signature) }));
     }
     this.triedConnect = true;
     String hostName = null;
     if (this.dInfo.isLocalConnection())
     {
       hostName = "localhost";
       try
       {
         this.sock = getConnected(hostName, this.dInfo.remoteCmdPort);
       }
       catch (IOException ex)
       {
         logger.except(ex);
         throw ex;
       }
     }
     else
     {
       hostName = this.dInfo.info.fullyQualifiedHostName == null ? this.dInfo.info.hostName : this.dInfo.info.fullyQualifiedHostName;
       try
       {
         this.sock = getConnected(hostName, this.dInfo.remoteCmdPort);
       }
       catch (IOException ex)
       {
         String msg = ex.getMessage();
         if (msg == null) {
           msg = ex.getClass().getSimpleName();
         }
         logger.info("Caught exception trying to connect to '%1$s': %2$s", new Object[] { hostName, msg });
         if ((hostName.equals(this.dInfo.info.fullyQualifiedHostName)) && (!hostName.equals(this.dInfo.info.hostName)))
         {
           hostName = this.dInfo.info.hostName;
           logger.debug("Fully-qualified name didn't work, trying '%1$s' instead...", new Object[] { hostName });
           try
           {
             this.sock = getConnected(hostName, this.dInfo.remoteCmdPort);
           }
           catch (IOException ex2)
           {
             if (this.dInfo.hostAddress != null)
             {
               hostName = this.dInfo.hostAddress;
               logger.debug("Neither version of host name worked, trying address '%1$s' instead...", new Object[] { hostName });
               this.sock = getConnected(InetAddress.getByName(hostName), this.dInfo.remoteCmdPort);
             }
             else
             {
               throw ex2;
             }
           }
         }
         else if (this.dInfo.hostAddress != null)
         {
           hostName = this.dInfo.hostAddress;
           logger.debug("Host name didn't work, trying address '%1$s' instead...", new Object[] { hostName });
           this.sock = getConnected(InetAddress.getByName(hostName), this.dInfo.remoteCmdPort);
         }
         else
         {
           throw ex;
         }
       }
     }
     if (isConnected())
     {
       logger.debug("Now connected to %1$s:%2$d", new Object[] { hostName, Integer.valueOf(this.dInfo.remoteCmdPort) });
       this.sock.setTcpNoDelay(true);
       
       BufferedOutputStream bos = new BufferedOutputStream(this.sock.getOutputStream(), this.sock.getSendBufferSize());
       BufferedInputStream bis = new BufferedInputStream(this.sock.getInputStream(), this.sock.getReceiveBufferSize());
       DataOutputStream dos = new DataOutputStream(bos);
       DataInputStream dis = new DataInputStream(bis);
       if (!negotiateProtocol(dis, dos)) {
         return false;
       }
       this.os = new ObjectOutputStream(dos);
       this.os.flush();
       this.is = new ObjectInputStream(dis);
       this.lock = new ReentrantLock();
       
       this.sessionActive = startSession(this.is, this.os);
       return this.sessionActive;
     }
     return false;
   }
   
   /**
    * 协商协议
    * @param dis
    * @param dos
    * @return
    * @throws IOException
 * @throws InvalidKeySpecException 
 * @throws NoSuchPaddingException 
 * @throws NoSuchAlgorithmException 
 * @throws Exception 
 * @throws InvalidKeyException 
    */
   private boolean negotiateProtocol(DataInputStream dis, DataOutputStream dos)
     throws IOException
   {
     logger.debug("Sending NEGOTIATE request", new Object[0]);
     dos.write(this.commandChannel ? RemoteCommand.COMMAND_SIGNATURE : RemoteCommand.SIGNATURE);
     dos.writeInt(1);
     dos.writeShort(1);
     dos.writeShort(4);
     dos.writeInt(supProtoLvl);
     dos.writeShort(3);
     dos.writeShort(4);
     dos.writeInt(supProtoMinorLvl);
     dos.writeShort(0);
     dos.writeShort(0);
     dos.flush();
     
     byte[] sig = new byte[4];
     dis.readFully(sig);
     if (!Arrays.equals(sig, RemoteCommand.SIGNATURE))
     {
       logger.warn("Received invalid signature %1$s", new Object[] { Arrays.toString(sig) });
       return false;
     }
     int rqstID = dis.readInt();
     switch (rqstID)
     {
     case 2: 
       logger.debug("Received NEGOTIATE response", new Object[0]);
       break;
     case 0: 
       logger.warn("Received CLOSE request: server closed the connection", new Object[0]);
       return false;
     default: 
       logger.warn("Received invalid response ID: %1$d (expected %2$d)", new Object[] { Integer.valueOf(rqstID), Integer.valueOf(2) });
       
       return false;
     }
     
     int level = this.curProtoLvl;
     int minorLevel = this.curProtoMinorLvl;
     byte[] pubKey = null;
     for (;;)
     {
       short paramID = dis.readShort();
       short type = dis.readShort();
       switch (paramID)
       {
       case 0: 
         if (type == 0) {
           break;
         }
         logger.warn("Wrong value type for EOP parameter: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf((short) 0) }); break;
       case 1: 
         logger.debug("Received LEVEL parameter", new Object[0]);
         if (type != 4)
         {
           logger.warn("Wrong value type for LEVEL parameter: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf((short) 4) });
           
           return false;
         }
         level = dis.readInt();
         if ((level < 1) || (level > supProtoLvl))
         {
           logger.warn("Invalid protocol LEVEL: %1$d (min %2$d, max %3$d)", new Object[] { Integer.valueOf(level), Integer.valueOf(1), Integer.valueOf(supProtoLvl) });
           
           return false;
         }
         break;
       case 3: 
         logger.debug("Received LEVEL_MINOR parameter", new Object[0]);
         if (type != 4)
         {
           logger.warn("Wrong value type for LEVEL_MINOR parameter: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf((short) 4) });
           
           return false;
         }
         minorLevel = dis.readInt();
         break;
       case 2: 
         logger.debug("Received PUBKEY parameter", new Object[0]);
         if (type != 258)
         {
           logger.warn("Wrong value type for PUBKEY parameter: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf((short) 258) });
           
           return false;
         }
         pubKey = new byte[dis.readShort()];
         dis.readFully(pubKey);
         break;
       default: 
         logger.warn("Invalid NEGOTIATE response parameter: %1$d, type %2$d", new Object[] { Short.valueOf(paramID), Short.valueOf(type) });
         return false;
       }
       
       logger.info("Negotiated protocol levels: %1$d.%2$d", new Object[] { Integer.valueOf(level), Integer.valueOf(minorLevel) });
       this.curProtoLvl = level;
       this.curProtoMinorLvl = minorLevel;
       if (pubKey != null) {
         try {
			this.security = new RemoteSecurity(pubKey);
		} catch (Exception  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e){
             e.printStackTrace();
         }
       }
       return true;
     }
 
//     logger.info("Negotiated protocol levels: %1$d.%2$d", new Object[] { Integer.valueOf(level), Integer.valueOf(minorLevel) });
//     this.curProtoLvl = level;
//     this.curProtoMinorLvl = minorLevel;
//     if (pubKey != null) {
//       this.security = new RemoteSecurity(pubKey);
//     }
//     return true;
   }
   
   /**
    * 开启会话
    * @param is
    * @param os
    * @return
    * @throws IOException
    * @throws IIapi.Exception
 * @throws BadPaddingException 
 * @throws IllegalBlockSizeException 
 * @throws ShortBufferException 
    */
   private boolean startSession(ObjectInputStream is, ObjectOutputStream os)
     throws IOException, IIapi.Exception
   {
     boolean close = false;
     if (this.curProtoMinorLvl < 0)
     {
       logger.info("Old server didn't send us a minor protocol level", new Object[0]);
       throw new IIapi.Exception(Intl.getString(RemoteCommand.pkg, "command.upgradeServer"));
     }
     logger.debug("Sending START SESSION request", new Object[0]);
     os.write(RemoteCommand.SIGNATURE);
     os.writeInt(3);
     //switch (1.$SwitchMap$com$ingres$SessionAuth$AuthType[this.sessionAuth.getAuthType().ordinal()])
     switch(this.sessionAuth.getAuthType().ordinal())
     {
     case 1: 
       os.writeShort(1);
       os.writeObject(this.sessionAuth.getUID());
       if (this.security != null)
       {
         byte[] auth = this.security.getUserAuth();
         os.writeShort(3);
         os.writeShort(auth.length);
         os.write(auth);
       }
       break;
     case 2: 
       os.writeShort(1);
       os.writeObject(this.sessionAuth.getUID());
       if (this.security != null)
       {
         byte[] pwd = this.sessionAuth.getPWD();
         pwd = this.security.encode(pwd);
         os.writeShort(2);
         os.writeShort(pwd.length);
         os.write(pwd);
         
         byte[] dbmsPwd = this.sessionAuth.getDBMSPWD();
         if (dbmsPwd != null)
         {
           dbmsPwd = this.security.encode(dbmsPwd);
           os.writeShort(4);
           os.writeShort(dbmsPwd.length);
           os.write(dbmsPwd);
         }
       }
       break;
     }
     os.writeShort(0);
     os.flush();//flushes the output stream and forces any buffered output bytes to be written out
     
     byte[] sig = new byte[4];
     is.readFully(sig);
     if (!Arrays.equals(sig, RemoteCommand.SIGNATURE))
     {
       logger.warn("Received invalid signature %1$s", new Object[] { Arrays.toString(sig) });
       throw new IIapi.Exception(Intl.getString(RemoteCommand.pkg, "function.invalidResponseSig"));
     }
     int rqstID = is.readInt();
     IIapi.Exception ex;
     switch (rqstID)
     {
     case 4: 
       logger.debug("Received SESSION START response", new Object[0]);
       
       ex = readException(is);
       if (ex != null) {
         throw ex;
       }
       this.instID = is.readInt();
       break;
     case 0: 
       logger.warn("Received CLOSE request: server closed the connection", new Object[0]);
       
       ex = readException(is);
       if (ex != null) {
         throw ex;
       }
       return false;
     default: 
       logger.warn("Received invalid response ID: %1$d (expected %2$d)", new Object[] { Integer.valueOf(rqstID), Integer.valueOf(4) });
       
       throw new IIapi.Exception(Intl.formatString(RemoteCommand.pkg, "connect.invalidResponseID", new Object[] { Integer.valueOf(rqstID) }));
     }
     logger.debug("Session started: installation ID = %1$x", new Object[] { Integer.valueOf(this.instID) });
     return true;
   }
   
   /**
    * 断开连接
 * @throws IOException 
    */
   public void disconnect() 
		   throws IOException
   {
     if (this.sock == null)
     {
       if (this.triedConnect) {
         return;
       }
       throw new IllegalStateException(Intl.getString(RemoteCommand.pkg, "connect.needDisconnect"));
     }
     String hostName = this.dInfo.info.fullyQualifiedHostName == null ? this.dInfo.info.hostName : this.dInfo.info.fullyQualifiedHostName;
     logger.info("Disconnecting from %1$s:%2$d...", new Object[] { hostName, Integer.valueOf(this.dInfo.remoteCmdPort) });
     int instID = this.instID;
     this.instID = 0;
     try
     {
       boolean closed = false;
       if (this.sessionActive)
       {
         this.sessionActive = false;
         this.os.write(RemoteCommand.SIGNATURE);
         this.os.writeInt(5);
         this.os.writeInt(instID);
         this.os.writeShort(0);
         this.os.flush();
         
         byte[] sig = new byte[4];
         this.is.readFully(sig);
         if (!Arrays.equals(sig, RemoteCommand.SIGNATURE))
         {
           logger.warn("Received invalid signature %1$s", new Object[] { Arrays.toString(sig) });
         }
         else
         {
           int rqstID = this.is.readInt();
           switch (rqstID)
           {
           case 6: 
             logger.debug("Received END SESSION response", new Object[0]);
             readException(this.is);
             break;
           case 0: 
             logger.warn("Received CLOSE request: server closed the connection", new Object[0]);
             closed = true;
             readException(this.is);
             break;
           default: 
             logger.warn("Received invalid response ID: %1$d (expected %2$d)", new Object[] { Integer.valueOf(rqstID), Integer.valueOf(6) });
           }
         }
       }
       if ((!closed) && (this.os != null))
       {
         this.os.write(RemoteCommand.SIGNATURE);
         this.os.writeInt(0);
         this.os.writeInt(0);
         this.os.flush();
       }
     }
     catch (Exception e)
     {
       throw new RuntimeException(e);
     }
     finally
     {
       try
       {
         this.sock.close();
       }
       catch (Exception ignore) {}
       this.sock = null;
       this.triedConnect = false;
     }
   }
   
   private IIapi.Exception readException(ObjectInputStream is)
     throws IOException
   {
     IIapi.Exception ex = null;
     
     int exceptCount = is.readInt();
     if (exceptCount != 0)
     {
       if (exceptCount != 1) {
         logger.warn("Invalid exception count: %1$d", new Object[] { Integer.valueOf(exceptCount) });
       }
       try
       {
         ex = (IIapi.Exception)is.readObject();
       }
       catch (ClassNotFoundException cnfe)
       {
         ex = new IIapi.Exception(cnfe.getMessage());
       }
       logger.except(ex);
     }
     return ex;
   }
   
   public boolean reset()
     throws IOException, IIapi.Exception
   {
     if (this.ci.update())
     {
       this.sock = null;
       this.triedConnect = false;
       this.is = null;
       this.os = null;
       this.curProtoLvl = 1;
       this.curProtoMinorLvl = -1;
       this.instID = 0;
       this.security = null;
       this.sessionActive = false;
       
       this.dInfo = this.ci.getDiscoveryInfo();
       this.sessionAuth = this.ci.getSessionAuth();
       
       logger.debug("RemoteConnection.reset: port changed to %1$d, resetting for retry...", new Object[] { Integer.valueOf(this.dInfo.remoteCmdPort) });
       
       return true;
     }
     return false;
   }
   
   public boolean lock()
   {
     try
     {
       this.lock.lockInterruptibly();
       return this.lock.isHeldByCurrentThread();
     }
     catch (InterruptedException ie) {}
     return false;
   }
   
   public void unlock()
   {
     if (this.lock.isHeldByCurrentThread()) {
       this.lock.unlock();
     }
   }
   
   public InputStream getRemoteFileInputStream(String name)
   {
     InputStream in = null;
     try
     {
       if (this.sessionActive)
       {
         lock();
         this.os.reset();
         this.os.write(RemoteCommand.SIGNATURE);
         this.os.writeInt(9);
         this.os.writeUTF(name);
         this.os.flush();
         
         byte[] sig = new byte[4];
         this.is.readFully(sig);
         if (!Arrays.equals(sig, RemoteCommand.SIGNATURE))
         {
           logger.warn("Did not receive signature response", new Object[0]);
           unlock();
           return null;
         }
         int rspId = this.is.readInt();
         int count = this.is.readInt();
         if (rspId != 10) {
           logger.warn("Received invalid response ID: %1$d (expected %2$d)", new Object[] { Integer.valueOf(rspId), Integer.valueOf(10) });
         }
         if (count != 0)
         {
           IIapi.Exception ex = (IIapi.Exception)this.is.readObject();
           logger.except(ex);
           unlock();
           throw ex;
         }
         this.remoteFileSize = this.is.readLong();
         
 
         logger.debug("getRemoteFileInputStream(%1$s) -> size %2$d", new Object[] { name, Long.valueOf(this.remoteFileSize) });
         in = new GZIPInputStream(this.is, 32768);
       }
     }
     catch (IIapi.Exception ie)
     {
       unlock();
       throw ie;
     }
     catch (Throwable e)
     {
       unlock();
       logger.except(e);
     }
     return in;
   }
   
   public void releaseRemoteFileStream()
   {
     unlock();
   }
   
   public long getRemoteFileSize()
   {
     return this.remoteFileSize;
   }
 }
