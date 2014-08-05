 package com.ingres.server;
 
 import com.ingres.ConnectOptions;
import com.ingres.ConnectionHandle;
import com.ingres.ContextHandle;
import com.ingres.IIapi;
import com.ingres.IIapi.Exception;
import com.ingres.IIapi.SysInfo;
import com.ingres.LocalIIapi;
import com.ingres.SessionAuth;
import com.ingres.discovery.Discovery;
import com.ingres.discovery.Discovery.Info;
import com.ingres.util.CharUtil;
import com.ingres.util.ClientStatistics;
import com.ingres.util.CountedInputStream;
import com.ingres.util.CountedOutputStream;
import com.ingres.util.Intl;
import com.ingres.util.LogStream;
import com.ingres.util.Logging;
import com.ingres.util.NetworkUtil;
import com.ingres.util.QueuedThread;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ServerSocketFactory;
 import javax.xml.crypto.Data;

 public class RemoteCommand
   implements Runnable
 {
   public static final byte[] SIGNATURE = { -80, 51, -6, 106 };
   public static final byte[] COMMAND_SIGNATURE = { 82, -104, 94, 9 };
   public static final byte[] PING_SIGNATURE = { 32, -47, 1, -51 };
   public static final byte[] MASTER_SHUTDOWN_SIGNATURE = { -34, -104, -76, -95 };

   private static final String SERVER_OK = "Server OK";
   private static final String SERVER_STOPPING = "Stopping";
   private static final String SERVER_SETTINGS_FILE = "iimgmtsvr.dat"; //server配置文件
   private static volatile Properties actualPortProperties = null;
   private static final String CONFIG_PORT_KEY = "mgmt_server.tcp_ip.port";
   private static final String ACTUAL_PORT_KEY = "mgmt_server.actual.port";
   private static final String ENV_PORT_KEY = "II_MGMTSVR_CMD_PORT";
   public static final Package pkg = RemoteCommand.class.getPackage();
   private static final int AUTH_CONNECT_TIMEOUT = 5000;
   private static final int PING_SOCKET_TIMEOUT = 2000;
   public static final int SHUTDOWN_KILL = 1;
   
   //璇锋眰浠ｅ彿锛屽鎴风閫氳繃socket鍙戦�佸埌server,server鏍规嵁杩欎釜rqstID纭鍝竴绫绘搷浣�
   public static final int CLOSE_REQUEST = 0;
   public static final int NEGOTIATE_REQUEST = 1;
   public static final int NEGOTIATE_RESPONSE = 2;
   public static final int START_SESSION = 3;
   public static final int START_RESPONSE = 4;
   public static final int END_SESSION = 5;
   public static final int END_RESPONSE = 6;
   public static final int FUNCTION_REQUEST = 7;      //func request
   public static final int FUNCTION_RESPONSE = 8;     //func response
   public static final int REMOTE_FILE_REQUEST = 9;   //file request
   public static final int REMOTE_FILE_RESPONSE = 10; //file response
   
   public static final short PARAM_EOP = 0; 
   public static final short NEG_P_LEVEL = 1;
   public static final short NEG_P_PUBKEY = 2;
   public static final short NEG_P_LEVEL_MINOR = 3;
   public static final short SESS_P_UID = 1;
   public static final short SESS_P_PWD = 2;
   public static final short SESS_P_LCL = 3;
   public static final short SESS_P_DBMSPWD = 4;
   public static final short VAL_TYP_NONE = 0;
   public static final short VAL_TYP_BYTE = 1;
   public static final short VAL_TYP_SHORT = 2;
   public static final short VAL_TYP_INT = 4;
   public static final short VAL_TYP_LONG = 8;
   public static final short VAL_TYP_BOOL = 257;
   public static final short VAL_TYP_BYTES = 258;
   public static final short VAL_TYP_UTF = 259;
   public static final int PROTOCOL_LEVEL_01 = 1; 
   public static final int PROTOCOL_LEVEL_02 = 2;
   public static final int PROTOCOL_LEVEL_03 = 3;
   public static final int PROTOCOL_LEVEL_04 = 4;
   public static final int PROTOCOL_LEVEL_05 = 5;
   public static final int PROTOCOL_MINOR_LEVEL = 101;
   public static final int BUFFER_SIZE = 32768;
   private static boolean respondToHTTP = false;
   private static volatile boolean terminate = false;
   private static volatile boolean killing = false;
   private static volatile boolean running = false;
   private static volatile Thread mainThread;
   private static volatile int remotePort = 0;
   private static Logging logger = new Logging(RemoteCommand.class);
   private static volatile Server cmdServer = null;
   private static final Map<Thread, RemoteCommand> clientMap = new Hashtable<Thread, RemoteCommand>();
   private static BlockingQueue<QueuedThread> workerQueue; //TODO
   private ClientStatistics statObj = null;
   private String clientName = null;
   private Socket socket = null;
   private String remoteAddress = null;
   private boolean doingTerminate = false;
   private ObjectInputStream is = null;
   private ObjectOutputStream os = null;
   private boolean insideFunctionCall = false;
   private Thread thread = null;
   private boolean connectLimitReached = false;
   private static int supProtoLvl = 5;
   private int curProtoLvl = 1;
   private static int supProtoMinorLvl = 101;
   private int curProtoMinorLvl = -1;
   private boolean commandChannel = false;
   private boolean sessionActive = false;
   private SessionAuth sessionAuth = null;
   private RemoteSecurity security = null;
   private IIapi apiInst = null;
   private int instID = -1;
   
   public RemoteCommand(String name, Socket sock)
   {
     logger.info("Created new worker: \"%1$s\"", new Object[] { name });
     
     this.clientName = name;
     this.socket = sock;
     this.remoteAddress = sock.getRemoteSocketAddress().toString();
     logger.debug("Connected to client at %1$s", new Object[] { this.remoteAddress });

     //注册该客户端
     this.statObj = ClientStatistics.registerClient(this.clientName, this);
     
     //获取守护线程队列的头元素，即第一个线程
     QueuedThread workerThread = (QueuedThread)workerQueue.poll();
     if (workerThread == null)
     {
       //线程队列中线程已用完，达到最大连接限制
       this.connectLimitReached = true;
       run();
     }
     else
     {
       workerThread.submitWork(this);
     }
   }
   
   public String toString()
   {
     return String.format("%1$s: %2$s", new Object[] { this.clientName, this.remoteAddress });
   }
   
   public static RemoteCommand getCurrentClient()
   {
     return (RemoteCommand)clientMap.get(Thread.currentThread());
   }
   
   public static int setTerminate(int flags)
   {
     logger.debug("setTerminate called(flags=0x%1$08x)", new Object[] { Integer.valueOf(flags) });
     terminate = true;
     
     RemoteCommand client = getCurrentClient();
     if (client != null)
     {
       client.doingTerminate = true;
       logger.debug("Setting 'doingTerminate' on client %1$s", new Object[] { client });
     }
     if ((flags & 0x1) != 0) {
       killing = true;
     }
     int count = -1;
     if (cmdServer != null) {
       count = cmdServer.stop(flags);
     } else if (killing) {
       killClients();
     }
     logger.debug("setTerminate returning count of %1$d", new Object[] { Integer.valueOf(count) });
     return count;
   }
   
   public static boolean isMarkedToTerminate()
   {
     return terminate;
   }
   
   public static boolean isRunning()
   {
     return running;
   }
   
   private void writeErrorResponse(ObjectOutputStream os, int id, String key)
     throws IOException
   {
     os.write(SIGNATURE);
     os.writeInt(id);
     os.writeInt(1);
     os.writeObject(new IIapi.Exception(Intl.getString(pkg, key)));
     os.flush();
   }
   
   /**
    * 瀹炵幇Runable鎺ュ彛run()鏂规硶
    */
   public void run()
   {
     try
     {
       logger.debug("Remote Command worker \"%1$s\" starting", new Object[] { this.clientName });
       this.socket.setTcpNoDelay(true);
       
       this.thread = Thread.currentThread();
       clientMap.put(this.thread, this);

       BufferedInputStream bis = new BufferedInputStream(this.socket.getInputStream(), this.socket.getReceiveBufferSize());
       BufferedOutputStream bos = new BufferedOutputStream(this.socket.getOutputStream(), this.socket.getSendBufferSize());
       DataInputStream dis = new DataInputStream(new CountedInputStream(bis, this.statObj));
       DataOutputStream dos = new DataOutputStream(new CountedOutputStream(bos, this.statObj));

       if (!negotiateProtocol(dis, dos)) {
         return;
       }
       this.is = new ObjectInputStream(dis);
       this.os = new ObjectOutputStream(dos);
       this.os.flush();
       byte[] sig = new byte[4];
       
       //
       for (;;)
       {
         this.is.readFully(sig);
         if (!Arrays.equals(sig, SIGNATURE))
         {
           logger.warn("Received invalid signature: %1$s", new Object[] { Arrays.toString(sig) });
           writeErrorResponse(this.os, 0, "command.invalidRequestSig");
           break;
         }
         
         //rqstID 解析到的命令ID
         int rqstID = this.is.readInt();
         switch (rqstID)
         {
         case 0:
           logger.debug("Received CLOSE request", new Object[0]);
           break;
         case 3:
           logger.debug("Received START SESSION request", new Object[0]);
           if (this.sessionActive)
           {
             writeErrorResponse(this.os, 4, "command.secondStart");
           }
           else
           {
             if (this.connectLimitReached)
             {
               logger.warn("Reached connect limit, aborting this session.", new Object[0]);
               writeErrorResponse(this.os, 4, "command.connectLimitReached");
               //break label892;
               break;
             }
             

             this.sessionActive = startSession(this.is, this.os);
           }
           break;
         case 5:
           logger.debug("Received END SESSION request", new Object[0]);
           if (!this.sessionActive)
           {
             writeErrorResponse(this.os, 6, "command.endWhenNotActive");
           }
           else
           {
             this.sessionActive = false;
             endSession(this.is, this.os);
           }
           break;
         case 7: // rqstID=7浠ｈ〃鍚勭鍑芥暟鎿嶄綔createdb, query绛夛紟銆�瀹㈡埛绔痵ocket鍙戦�佹暟鎹彲鍙傝�傾PIFunction.call()
           //createdb,query绛夊懡浠ゆ搷浣�
           logger.debug("Received FUNCTION request", new Object[0]);
           if (!this.sessionActive)
           {
             writeErrorResponse(this.os, 8, "command.noSessionActive");
           }
           else
           {
             this.insideFunctionCall = true;
             try
             {
            	 //瑙﹀彂鎿嶄綔 createdb,query绛�
               invokeFunction(this.is, this.os);
             } catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
             finally
             {
               this.insideFunctionCall = false;
             }
           }
           break;
         case 9: 
           //杩滅▼鏂囦欢璇锋眰銆�
           logger.debug("Received REMOTE_FILE request", new Object[0]);
           if (!this.sessionActive)
           {
             writeErrorResponse(this.os, 10, "command.noSessionActive");
           }
           else
           {
             String name = this.is.readUTF();
             
             FileInputStream fileInputStream = null;
             try
             {
               fileInputStream = new FileInputStream(name);
             }
             catch (FileNotFoundException fnfe)
             {
               logger.warn("Remote file not found: %1$s", new Object[] { fnfe.getMessage() });
               writeErrorResponse(this.os, 10, "command.fileNotFound");
               fileInputStream = null;
               break;
             }
             logger.debug("Starting remote file transfer", new Object[0]);
             this.os.write(SIGNATURE);
             this.os.writeInt(10);
             this.os.writeInt(0);
             this.os.writeLong(fileInputStream.available());
             
             byte[] buf = new byte[32768];
             
             GZIPOutputStream gzOut = new GZIPOutputStream(this.os, 32768);
             int len;
             while ((len = fileInputStream.read(buf)) >= 0) {
               gzOut.write(buf, 0, len);
             }
             gzOut.finish();
             gzOut.flush();
             gzOut = null;
             this.os.flush();
             buf = null;
             
             fileInputStream.close();
             fileInputStream = null;
           }
           break;
         case 1: 
         case 2: 
         case 4: 
         case 6: 
         case 8: 
         default: 
        //鏃犳晥璇锋眰
           logger.warn("Received invalid request ID: %1$d", new Object[] { Integer.valueOf(rqstID) });
           writeErrorResponse(this.os, 0, "command.invalidRequestID");
           //break label892;
           break;
         }
       }
     }
     catch (ParameterException pe)
     {
       logger.except(pe);
     }
     catch (IllegalArgumentException iae)
     {
       logger.except(iae);
     }
     catch (EOFException eof) {}catch (SocketException socketexception) {}catch (IOException ioe)
     {
       logger.except(ioe);
     }
     catch (Exception exc)
     {
       //label892:
       logger.except(exc);
     } catch (InvalidKeyException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (NoSuchAlgorithmException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (NoSuchPaddingException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IllegalBlockSizeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BadPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     finally
     {
       logger.debug("Remote Command worker \"%1$s\" exiting...", new Object[] { this.clientName });
       if (this.sessionActive) {
         freeSessionResources();
       }
       try
       {
         this.socket.close();
       }
       catch (IOException ignore) {}
       this.socket = null;
       clientMap.remove(this.thread);
       this.thread = null;
       this.statObj.unregisterClient();
       this.statObj = null;
       logger.debug("Remote Command worker \"%1$s\" finished.", new Object[] { this.clientName });
     }
   }
   
   /**
    * 鍗忚鍗忓晢
    * @param dis
    * @param dos
    * @return
    * @throws EOFException
    * @throws IOException
 * @throws NoSuchPaddingException 
 * @throws NoSuchAlgorithmException 
 * @throws Exception 
 * @throws InvalidKeyException 
    */
   private boolean negotiateProtocol(DataInputStream dis, DataOutputStream dos)
     throws EOFException, IOException, InvalidKeyException, Exception, NoSuchAlgorithmException, NoSuchPaddingException
   {
     byte[] sig = new byte[4];
     dis.readFully(sig);
     logger.debug("Received signature: %1$s", new Object[] { Arrays.toString(sig) });
     if (Arrays.equals(sig, COMMAND_SIGNATURE))
     {
       this.commandChannel = true;
     }
     else if (!Arrays.equals(sig, SIGNATURE))
     {
       if (((sig[0] == 71) && (sig[1] == 69) && (sig[2] == 84) && (sig[3] == 32)) || ((sig[0] == 80) && (sig[1] == 79) && (sig[2] == 83) && (sig[3] == 84)) || ((sig[0] == 72) && (sig[1] == 69) && (sig[2] == 65) && (sig[3] == 68)) || ((sig[0] == 80) && (sig[1] == 85) && (sig[2] == 84) && (sig[3] == 32)))
       {
         if (respondToHTTP)
         {
           String localHostName = "localhost";
           try
           {
             localHostName = InetAddress.getLocalHost().getCanonicalHostName();
           }
           catch (UnknownHostException e) {}
           String response = String.format("HTTP/1.0 200 OK\r\nServer: %1$s\r\n\r\n<html><body><h1>Hello from %1$s!</h1></body></html>", new Object[] { localHostName });
           dos.writeBytes(response);
         }
         else
         {
           logger.info("Returning HTTP \"Bad Request\" response", new Object[0]);
           dos.writeBytes("HTTP/1.0 400 Bad Request\r\n\r\n");
         }
         dos.flush();
       }
       else if (Arrays.equals(sig, PING_SIGNATURE))
       {
         logger.debug("Sending ping response", new Object[0]);
         
         dos.writeUTF("Server OK");
         dos.flush();
       }
       else if (Arrays.equals(sig, MASTER_SHUTDOWN_SIGNATURE))
       {
         logger.debug("Received master shutdown signal", new Object[0]);
         if (terminate) {
           dos.writeUTF("Stopping");
         } else {
           dos.writeUTF("Server OK");
         }
         dos.flush();
         mainThread.interrupt();
       }
       else
       {
         logger.warn("Invalid remote command signature received!", new Object[0]);
       }
       return false;
     }
     if ((terminate) && (!this.commandChannel)) {
       return false;
     }
     int rqstID = dis.readInt();
     switch (rqstID)
     {
     case 1: 
       logger.debug("Received NEGOTIATE request", new Object[0]);
       break;
     case 0: 
       logger.debug("Received CLOSE request", new Object[0]);
       return false;
     default: 
       logger.warn("Received invalid request ID: %1$d (expected %2$d)", new Object[] { Integer.valueOf(rqstID), Integer.valueOf(1) });
       
       dos.write(SIGNATURE);
       dos.writeInt(0);
       dos.writeInt(0);
       return false;
     }
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
         logger.warn("Wrong value type for EOP parameter: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf("0") }); break;
       case 1: 
         logger.debug("Received LEVEL parameter", new Object[0]);
         if (type != 4)
         {
           logger.warn("Wrong value type for protocol LEVEL: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf("4") });
           
           dos.write(SIGNATURE);
           dos.writeInt(0);
           dos.writeInt(0);
           return false;
         }
         int level = dis.readInt();
         if (level < 1)
         {
           logger.warn("Invalid protocol LEVEL: %1$d", new Object[] { Integer.valueOf(level) });
           dos.write(SIGNATURE);
           dos.writeInt(0);
           dos.writeInt(0);
           return false;
         }
         this.curProtoLvl = Math.min(level, supProtoLvl);
         logger.debug("Negotiated protocol level: %1$d", new Object[] { Integer.valueOf(this.curProtoLvl) });
         break;
       case 3: 
         logger.debug("Received LEVEL_MINOR parameter", new Object[0]);
         if (type != 4)
         {
           logger.warn("Wrong value type for protocol LEVEL_MINOR: %1$d (should be %2$d)", new Object[] { Short.valueOf(type), Short.valueOf("4") });
           
           dos.write(SIGNATURE);
           dos.writeInt(0);
           dos.writeInt(0);
           return false;
         }
         this.curProtoMinorLvl = dis.readInt();
         
         logger.debug("Received protocol minor level: %1$d", new Object[] { Integer.valueOf(this.curProtoMinorLvl) });
         break;
       case 2: 
       default: 
         logger.debug("Skipping unknown parameter: %1$d, type %2$d", new Object[] { Short.valueOf(paramID), Short.valueOf(type) });
         switch (type)
         {
         case 0: 
           break;
         case 257: 
           dis.readBoolean(); break;
         case 1: 
           dis.readByte(); break;
         case 2: 
           dis.readShort(); break;
         case 4: 
           dis.readInt(); break;
         case 8: 
           dis.readLong(); break;
         case 259: 
           dis.readUTF(); break;
         case 258: 
           short len = dis.readShort();
           dis.skipBytes(len);
           break;
         default: 
           logger.warn("Invalid parameter type: %1$d", new Object[] { Short.valueOf(type) });
           dos.write(SIGNATURE);
           dos.writeInt(0);
           dos.writeInt(0);
           return false;
         }
         break;
       }
     
     //label1029:
     this.security = new RemoteSecurity();
     byte[] key = this.security.getEncodedKey();
     
     dos.write(SIGNATURE);
     dos.writeInt(2);
     dos.writeShort(1);
     dos.writeShort(4);
     dos.writeInt(this.curProtoLvl);
     if (this.curProtoMinorLvl >= 0)
     {
       dos.writeShort(3);
       dos.writeShort(4);
       dos.writeInt(this.curProtoMinorLvl);
     }
     dos.writeShort(2);
     dos.writeShort(258);
     dos.writeShort(key.length);
     dos.write(key);
     dos.writeShort(0);
     dos.writeShort(0);
     dos.flush();
     
     logger.debug("Protocol negotiation successful", new Object[0]);
     return true;
     }
   }
   
   /**
    * 寮�鍚細璇�
 * @throws BadPaddingException 
 * @throws IllegalBlockSizeException 
    */
   private boolean startSession(ObjectInputStream is, ObjectOutputStream os)
     throws IOException, IllegalBlockSizeException, BadPaddingException
   {
     IIapi.Exception ex = null;
     String uid = null;
     byte[] pwd = null;
     byte[] lcl = null;
     byte[] dbmsPwd = null;
     
     for (;;)
     {
       int paramID = is.readShort();
       switch (paramID)
       {
       case 0: 
         break;
       case 1: 
         try
         {
           uid = (String)is.readObject();
           logger.debug("Received UID parameter: %1$s", new Object[] { uid });
         }
         catch (ClassNotFoundException cnfe)
         {
           logger.warn("Couldn't read UID parameter!", new Object[0]);
           ex = new IIapi.Exception(cnfe.getMessage());
         }
       case 2: 
         pwd = new byte[is.readShort()];
         is.readFully(pwd);
         logger.debug("Received PWD parameter", new Object[0]);
         break;
       case 3: 
         lcl = new byte[is.readShort()];
         is.readFully(lcl);
         logger.debug("Received LCL parameter", new Object[0]);
         break;
       case 4: 
         dbmsPwd = new byte[is.readShort()];
         is.readFully(dbmsPwd);
         logger.debug("Received DBMSPWD parameter", new Object[0]);
         break;
       default: 
         logger.warn("Received invalid parameter type: %1$d", new Object[] { Integer.valueOf(paramID) });
         ex = new IIapi.Exception(Intl.getString(pkg, "command.unknownStartParam"));
       }
     
     
     if (ex == null) 
     {
       if (uid != null)
       {
         this.sessionAuth = null;
         if (pwd != null)
         {
           pwd = this.security.decode(pwd);
           if (dbmsPwd != null) {
             dbmsPwd = this.security.decode(dbmsPwd);
           }
           try
           {
             validatePassword(uid, pwd, dbmsPwd);
             if (SessionAuth.isProcessUID(uid)) {
               this.sessionAuth = SessionAuth.getAuth();
             } else if (dbmsPwd != null) {
               this.sessionAuth = SessionAuth.getAuth(uid, pwd, dbmsPwd);
             } else {
               this.sessionAuth = SessionAuth.getAuth(uid, pwd);
             }
           }
           catch (IIapi.Exception apiEx)
           {
             logger.warn("Password validation failed!", new Object[0]);
             ex = apiEx;
           }
         }
         else if (lcl != null)
         {
           if (!NetworkUtil.isLocalConnection(this.socket))
           {
             logger.warn("Local authorization not allowed on remote connections!", new Object[0]);
             ex = new IIapi.Exception(Intl.getString(pkg, "command.userPassRequired"));
           }
           else if (!this.security.validateUserAuth(uid, lcl))
           {
             logger.warn("Local authorization validation failed!", new Object[0]);
             ex = new IIapi.Exception(Intl.formatString(pkg, "command.invalidLocalAuth", new Object[] { uid }));
           }
           else if (SessionAuth.isProcessUID(uid))
           {
             this.sessionAuth = SessionAuth.getLimitedAuth();
           }
           else
           {
             this.sessionAuth = SessionAuth.getAuth(uid);
           }
         }
         else
         {
           ex = new IIapi.Exception(Intl.formatString(pkg, "command.noAuth", new Object[] { uid }));
         }
       }
       else if ((pwd != null) || (lcl != null))
       {
         this.sessionAuth = null;
         ex = new IIapi.Exception(Intl.getString(pkg, "command.notEnoughAuth"));
       }
     }
     
     if ((ex == null) && (this.sessionAuth == null)) {
       ex = new IIapi.Exception(Intl.getString(pkg, "command.authRequired"));
     }
     if (this.curProtoMinorLvl != supProtoMinorLvl)
     {
       logger.error("Minor protocol mismatch: client=%1$d, server(us)=%2$d", new Object[] { Integer.valueOf(this.curProtoMinorLvl), Integer.valueOf(supProtoMinorLvl) });
       if (this.curProtoMinorLvl < supProtoMinorLvl) {
         ex = new IIapi.Exception(Intl.getString(pkg, "command.upgradeClient"));
       } else {
         ex = new IIapi.Exception(Intl.getString(pkg, "command.upgradeServer"));
       }
     }
     if (ex == null) {
       try
       {
         this.apiInst = new LocalIIapi(this.sessionAuth);
       }
       catch (Exception e)
       {
         ex = new IIapi.Exception(e.getMessage());
       }
     }
     os.write(SIGNATURE);
     os.writeInt(4);
     if (ex != null)
     {
       logger.warn("Error starting session: " + ex.getMessage(), new Object[0]);
       os.writeInt(1);
       os.writeObject(ex);
       os.flush();
       return false;
     }
     this.instID = this.apiInst.hashCode();
     
     os.writeInt(0);
     os.writeInt(this.instID);
     os.flush();
     logger.debug("Session started", new Object[0]);
     return true;
    }
}
   
   /**
    * 瀵嗙爜楠岃瘉
    * @param uid
    * @param pwd
    * @param dbmsPwd
    * @throws IIapi.Exception
    */
   private void validatePassword(String uid, byte[] pwd, byte[] dbmsPwd)
     throws IIapi.Exception
   {
     IIapi inst;
     try
     {
       inst = new LocalIIapi();
     }
     catch (IIapi.Exception apiEx)
     {
       throw apiEx;
     }
//     catch (Exception ex)
//     {
//       throw new IIapi.Exception(ex.getMessage());
//     }
     
     try
     {
       if (dbmsPwd != null)
       {
         ConnectOptions options = new ConnectOptions(null, null, null, CharUtil.getUtf8String(dbmsPwd));
         
         ConnectionHandle handle = null;
         try
         {
           handle = inst.connectDatabase("iidbdb", uid, pwd, 5000, options);
         }
         finally
         {
           if (handle != null) {
             inst.disconnect(handle);
           }
         }
       }
       else
       {
         ConnectionHandle handle = null;
         try
         {
           handle = inst.connectDatabase("iidbdb", uid, pwd, 5000);
         }
         catch (Throwable ex)
         {
           ConnectOptions options = new ConnectOptions(null, null, null, CharUtil.getUtf8String(pwd));
           try
           {
             byte[] bogus = new byte[2];
             bogus[0] = 1;
             bogus[1] = 127;
             handle = inst.connectDatabase("iidbdb", uid, bogus, 5000, options);
           }
           catch (Throwable ex2)
           {
             inst.GCusrpwd(uid, pwd);
           }
         }
         finally
         {
           if (handle != null) {
             inst.disconnect(handle);
           }
         }
       }
     }
     catch (IIapi.Exception apiEx)
     {
       throw apiEx;
     }
//     catch (Exception ex)
//     {
//       throw new IIapi.Exception(ex.getMessage());
//     }
     finally
     {
       inst.unloadInstallation();
     }
   }
   
   private void endSession(ObjectInputStream is, ObjectOutputStream os)
     throws IOException
   {
     int instID = is.readInt();
     for (;;)
     {
       int paramID = is.readShort();
       switch (paramID)
       {
       case 0: 
         break;
       default: 
         logger.warn("Received invalid parameter type: %1$d", new Object[] { Integer.valueOf(paramID) });
       }
     
     
     if (this.instID != instID) {
       logger.warn("Invalid API instance ID: expected %1$x, got %2$x", new Object[] { Integer.valueOf(this.instID), Integer.valueOf(instID) });
     }
     
     freeSessionResources();
     
     os.write(SIGNATURE);
     os.writeInt(6);
     os.writeInt(0);
     os.flush();
     logger.debug("Session ends", new Object[0]);
     }
   }
   
   private void freeSessionResources()
   {
     this.apiInst.unloadInstallation();
     this.apiInst = null;
     this.instID = -1;
   }
   
   /**
    *
    * @param is
    * @param
    * @throws IllegalArgumentException
    * @throws ParameterException
    * @throws EOFException
    * @throws IOException
 * @throws IllegalAccessException 
 * @throws InstantiationException 
    */
   private void invokeFunction(ObjectInputStream is, ObjectOutputStream os)
     throws IllegalArgumentException, ParameterException, EOFException, IOException, InstantiationException, IllegalAccessException
   {
     os.reset();
     
     int instID = is.readInt();//瀹炰緥id
     int functionID = is.readInt(); //鍛戒护id
     short functionVersion = is.readShort(); //鐗堟湰淇℃伅
     int numParameters = is.readInt(); //鍙傛暟
     
     logger.debug("Received values: instance=%1$x, function=%2$x, version=%3$d, num params=%4$d", new Object[] { Integer.valueOf(instID), Integer.valueOf(functionID), Short.valueOf(functionVersion), Integer.valueOf(numParameters) });
     APIFunction f;
     try
     {
    	 //鏍规嵁鍛戒护id鑾峰彇鍛戒护
       f = APIFunction.getFunction(functionID, functionVersion);
     }
     catch (RuntimeException ex)
     {
       os.write(SIGNATURE);
       os.writeInt(8);
       os.writeInt(1);
       os.writeObject(ex);
       os.flush();
       
       throw ex;
     }
     logger.debug("Function '%1$s' identified.", new Object[] { f.displayName() });
     
     Parameter[] formalParams = f.getFormalParameters();
     Parameter[] actualParams = Parameter.cloneParameters(formalParams);
     if (numParameters != formalParams.length) {
       logger.error("Parameter length mismatch: expecting %1$d, got %2$d", new Object[] { Integer.valueOf(formalParams.length), Integer.valueOf(numParameters) });
     }
     for (Parameter p : actualParams) {
       p.read(is, Parameter.IO.INPUT, this.curProtoLvl);
     }
     if (this.instID != instID) {
       logger.error("API instance ID mismatch: expecting %1$x, got %2$x", new Object[] { Integer.valueOf(this.instID), Integer.valueOf(instID) });
     }
     Parameter ret = null;
     try
     {
    	 //瑙﹀彂鍛戒护銆�浣嗘槸杩欓噷invoke鍑芥暟涓篴bstract鍑芥暟锛屾湭鎵惧埌瀹炵幇鍐呭
       logger.debug("Invoking function '%1$s' on installation %2$x...", new Object[] { f.displayName(), Integer.valueOf(instID) });
       ret = f.invoke(this.apiInst, actualParams);//todo
       
       logger.debug("Writing 'no exceptions' indicator", new Object[0]);
       os.write(SIGNATURE);
       os.writeInt(8); //func call response 杩斿洖瀹㈡埛绔痵ocket: 杩欐槸func call鐨勬墽琛岀粨鏋�
       os.writeInt(0);
       
       logger.debug("Writing back output and input/output values", new Object[0]);
       for (Parameter p : actualParams) {
         p.write(os, Parameter.IO.OUTPUT, this.curProtoLvl);
       }
       logger.debug("Writing return value", new Object[0]);
       if (ret != null) {
         ret.write(os, Parameter.IO.OUTPUT, this.curProtoLvl);
       }
     }
     catch (RuntimeException ex)
     {
       logger.except(f.displayName(), ex);
       
       logger.debug("Writing 'have one exception' indicator", new Object[0]);
       os.write(SIGNATURE);
       os.writeInt(8);
       os.writeInt(1);
       os.writeObject(ex);
       if (this.curProtoLvl <= 1) {
         //break label588;
       }
     }
     for (Parameter p : actualParams) {
       if (p.dir == Parameter.IO.INOUT) {
         p.write(os, Parameter.IO.OUTPUT, this.curProtoLvl);
       } else if ((this.curProtoLvl > 2) && 
         (p.dir == Parameter.IO.OUTPUT)) {
         p.write(os, Parameter.IO.OUTPUT, this.curProtoLvl);
       }
     }
     //label588:
     os.flush();
     logger.debug("Done with function '%1$s'", new Object[] { f.displayName() });
   }
   
   private static void killClients()
   {
     for (RemoteCommand client : clientMap.values()) {
       if (!client.doingTerminate) {
         try
         {
           if (client.socket != null) {
             client.socket.close();
           }
           if ((client.insideFunctionCall) && 
             (client.thread != null)) {
             client.thread.interrupt();
           }
         }
         catch (IOException ioe)
         {
           if (Logging.isDebugEnabled()) {
             logger.except("Socket close during 'killClients'", ioe);
           }
         }
       }
     }
   }
   
   public static void setRespondToHTTP(boolean respond)
   {
     respondToHTTP = respond;
   }
   
   /**
    * 鑾峰彇閰嶇疆鐨勭洃鍚鍙�
    * @param inst
    * @return
    */
   public static int getConfiguredListenPort(IIapi inst)
   {
     int port = 0;
     String portstr = inst.getEnv("II_MGMTSVR_CMD_PORT");
     if ((portstr == null) || (portstr.isEmpty()))
     {
       ContextHandle ctx = inst.PMinit();
       inst.PMload(ctx, (String)null);
       String key = String.format("%1$s.%2$s.%3$s", new Object[] { inst.PMgetDefault(ctx, 0), inst.PMgetDefault(ctx, 1), "mgmt_server.tcp_ip.port" });
       
       portstr = inst.PMget(ctx, key);
       inst.PMfree(ctx);
     }
     if ((portstr != null) && (!portstr.isEmpty())) {
       try
       {
         port = inst.GCtcpIpPort(portstr, 0);
         logger.debug("Configured remote server listen port is %1$s (%2$d)", new Object[] { portstr, Integer.valueOf(port) });
       }
       catch (IIapi.Exception iae)
       {
         logger.error("%1$s: %2$s", new Object[] { iae.getMessage(), portstr });
       }
     }
     return port;
   }
   
   /**
    *
    * @param inst
    * @return
    */
   public static int getActualListenPort(IIapi inst)
   {
     if (actualPortProperties == null)
     {
       Properties props = new Properties();
       try
       {
         File settingsFile = new File(inst.getEnv("II_CONFIG"), "iimgmtsvr.dat");
         if (settingsFile.exists())
         {
           FileReader reader = null;
           try
           {
             reader = new FileReader(settingsFile);
             props.load(reader);
           }
           finally
           {
             if (reader != null) {
               reader.close();
             }
           }
         }
       }
       catch (IOException ioe)
       {
         logger.except("getActualListenPort", ioe);
       }
       actualPortProperties = props;
     }
     int port = Integer.parseInt(actualPortProperties.getProperty("mgmt_server.actual.port", "-1"));
     if ((port <= 0) || (!pingServerPort(port)))
     {
       port = -1;
       setActualListenPort(inst, port);
     }
     return port;
   }
   
   /**
    *
    * @param inst
    * @param port
    */
   public static void setActualListenPort(IIapi inst, int port)
   {
     logger.debug("setActualListenPort to %1$d", new Object[] { Integer.valueOf(port) });
     File settingsFile = new File(inst.getEnv("II_CONFIG"), "iimgmtsvr.dat");
     if (port <= 0)
     {
       if (settingsFile.exists()) {
         settingsFile.delete();
       }
       actualPortProperties = null;
     }
     else
     {
       try
       {
         FileWriter writer = null;
         try
         {
           writer = new FileWriter(settingsFile);
           if (actualPortProperties == null) {
             actualPortProperties = new Properties();
           }
           actualPortProperties.setProperty("mgmt_server.actual.port", Integer.toString(port));
           actualPortProperties.store(writer, "iimgmtsvr actual listen port value");
         }
         finally
         {
           if (writer != null) {
             writer.close();
           }
         }
       }
       catch (IOException ioe)
       {
         logger.except("setActualListenPort", ioe);
       }
     }
   }
   
   private static boolean pingServerPort(int port)
   {
     logger.debug("pingServerPort(%1$d)", new Object[] { Integer.valueOf(port) });
     try
     {
       Socket sock = new Socket("localhost", port);
       sock.setTcpNoDelay(true);
       sock.setSoTimeout(2000);
       OutputStream outputStream = sock.getOutputStream();
       DataOutputStream os = new DataOutputStream(outputStream);
       InputStream inputStream = sock.getInputStream();
       DataInputStream is = new DataInputStream(inputStream);
       os.write(PING_SIGNATURE);
       os.flush();
       String response = is.readUTF();
       is.close();
       os.close();
       return response.equals("Server OK");
     }
     catch (SocketTimeoutException ste) {}catch (IOException ignored) {}
     return false;
   }
   
   public static boolean signalMasterShutdown(int port)
   {
     try
     {
       Socket sock = new Socket("localhost", port);
       sock.setTcpNoDelay(true);
       sock.setSoTimeout(2000);
       OutputStream outputStream = sock.getOutputStream();
       DataOutputStream os = new DataOutputStream(outputStream);
       InputStream inputStream = sock.getInputStream();
       DataInputStream is = new DataInputStream(inputStream);
       os.write(MASTER_SHUTDOWN_SIGNATURE);
       os.flush();
       String response = is.readUTF();
       is.close();
       os.close();
       return response.equals("Server OK");
     }
     catch (SocketTimeoutException ste)
     {
       logger.except("signalMasterShutdown", ste);
     }
     catch (IOException ignored) {}
     return false;
   }

//----------------------------------------------------------------------------------------------
   
   /**
    * Server绫�
    * @author zxbing
    *
    */
   public static class Server
     implements Runnable
   {
     private ServerSocket listenSocket = null;
     private IIapi inst = null;
     private int clientNumber = 0;
     
     public Server(ServerSocket servSock, IIapi inst)
     {
       this.listenSocket = servSock;
       this.inst = inst;
     }
     
     /**
      *
      */
     public void run()
     {
       RemoteCommand.logger.info("Server.run()", new Object[0]);
       try
       {
    	 //@modify zhengxb 这里根据log等上下文推测
         //RemoteCommand.access$102(true);
         RemoteCommand.running = true;
         RemoteCommand.logger.debug("Server.run: running = true", new Object[0]);
         
         int statisticsInterval = ServerProperties.getIntValue(ServerProperties.Property.MonitorStatisticsInterval);
         RemoteCommand.logger.info("Starting statistics monitoring thread on %1$d minute intervals.", new Object[] { Integer.valueOf(statisticsInterval) });
         ClientStatistics.startMonitorThread(statisticsInterval);
         
         int threadPoolSize = ServerProperties.getIntValue(ServerProperties.Property.ServerConnectLimit);
         if ((threadPoolSize < 1) || (threadPoolSize > 1000)) {
           threadPoolSize = 100;
         }
         RemoteCommand.logger.info("Starting %1$d worker threads...", new Object[] { Integer.valueOf(threadPoolSize) });
         
         //@modify zhengxb 这里只能推断一下
         //RemoteCommand.access$202(new ArrayBlockingQueue(threadPoolSize));
         RemoteCommand.workerQueue = new ArrayBlockingQueue<QueuedThread>(threadPoolSize);

         //创建守护线程，并把这些守护线程添加到线程队列
         for (int i = 0; i < threadPoolSize; i++) {
           new QueuedThread(RemoteCommand.workerQueue);
         }
         while (((!RemoteCommand.terminate) || (ClientStatistics.currentClientCount() != 0)) && (!RemoteCommand.killing)) {
           try
           {
             if (this.listenSocket == null)
             {
               RemoteCommand.logger.debug("Creating listenSocket again (port %1$d) to wait for possible kill.", new Object[] { Integer.valueOf(RemoteCommand.remotePort) });
               ServerSocketFactory factory = ServerSocketFactory.getDefault();
               this.listenSocket = factory.createServerSocket(RemoteCommand.remotePort);
               this.listenSocket.setSoTimeout(1000);
             }
             //等待客户端连接请求
             Socket socket = this.listenSocket.accept();
             
             //收到客户端连接请求
             this.clientNumber += 1;
             String clientName = String.format("Client %1$d", new Object[] { Integer.valueOf(this.clientNumber) });
             
             //从守护线程队列中取出一个线程，执行该客户端请求
             new RemoteCommand(clientName, socket);
           }
           catch (SocketTimeoutException ste) {}catch (SocketException se)
           {
             if (Logging.isDebugEnabled()) {
               RemoteCommand.logger.except("listenSocket got closed in main loop; leaving", se);
             }
           }
           catch (IOException e)
           {
             if (Logging.isDebugEnabled()) {
               RemoteCommand.logger.except("Inside main Server loop", e);
             }
           }
         }
         RemoteCommand.logger.debug("Server.run: done with main loop.", new Object[0]);
       }
       catch (Exception exception)
       {
         RemoteCommand.logger.except(exception);
       }
       finally
       {
         try {
			cleanup();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       }
     }
     
     public synchronized int stop(int flags)
     {
       if (this.listenSocket != null) {
         try
         {
           this.listenSocket.close();
           this.listenSocket = null;
         }
         catch (IOException ioe)
         {
           if (Logging.isDebugEnabled()) {
             RemoteCommand.logger.except("I/O Exception closing server listen socket", ioe);
           }
         }
       }
       if ((flags & 0x1) != 0) {
         //RemoteCommand.access$600();
    	  
       }
       return ClientStatistics.currentClientCount();
     }
     
     public synchronized void cleanup() throws IOException
     {
       RemoteCommand.logger.debug("Server.cleanup(), running = %1$s", new Object[] { Boolean.valueOf(RemoteCommand.running) });
       if (RemoteCommand.running)
       {
         ClientStatistics.dumpStatistics(new LogStream());
         if (this.listenSocket != null) {
           try
           {
             this.listenSocket.close();
           }
           catch (Exception ignore) {}
         }
         this.listenSocket = null;
         
         RemoteCommand.setActualListenPort(this.inst, -1);
         
         //RemoteCommand.access$102(false);
         RemoteCommand.running = false;
         
         RemoteCommand.mainThread.interrupt();//中断主线程
       }
       //RemoteCommand.access$802(null);
     }
   }

//-------------------------------------------------------------------------------------------   
   
   /**
    * 涓鸿繙绋嬪懡浠ゅ垱寤轰竴涓墽琛岀嚎绋嬶紵
    * @param inst iiapi实例
    * @param dInfo
    * @param mainThread
    * @return
    */
   public static Thread startThread(IIapi inst, Discovery.Info dInfo, Thread mainThread)
   {
	 //@modify zhengxb
     RemoteCommand.mainThread = mainThread;
     
     ServerSocketFactory factory = ServerSocketFactory.getDefault();
     ServerSocket servSock = null;
     try
     {
       //閰嶇疆鏂囦欢璇诲彇鐩戝惉绔彛
       remotePort = getConfiguredListenPort(inst);
       logger.debug("Configured port: %1$d", new Object[] { Integer.valueOf(remotePort) });
       if (remotePort == 0)
       {
         servSock = factory.createServerSocket();
         servSock.bind(null);
         remotePort = servSock.getLocalPort();
       }
       else
       {
         servSock = factory.createServerSocket(remotePort);
       }
     }
     catch (IllegalArgumentException iae)
     {
       logger.except(iae);
       return null;
     }
     catch (IOException ioe)
     {
       logger.except("Starting background RemoteCommand thread", ioe);
       return null;
     }
     dInfo.remoteCmdPort = remotePort;
     logger.info("Remote Command for installation %1$s listening on port %2$d", new Object[] { dInfo.info.IIinstallation, Integer.valueOf(remotePort) });
     
     setActualListenPort(inst, remotePort);
     
     //启动Server为一个守护线程
     cmdServer = new Server(servSock, inst);
     Thread serverThread = new Thread(cmdServer, "RemoteServer");
     serverThread.setDaemon(true);//设置为守护线程
     serverThread.start();
     return serverThread;
   }
   
   public static void cleanup() throws IOException
   {
     if (cmdServer != null) {
       cmdServer.cleanup();
     }
   }
 }
