 package com.ingres.util;
 
 import java.io.IOException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.Socket;
 import java.net.SocketAddress;
 import java.net.UnknownHostException;
 
 public class NetworkUtil
 {
   private static InetAddress localv4;
   private static InetAddress localv6;
   public static final String LOCAL_PREFIX = "local";
   public static final String LOCALHOST = "localhost";
   
   static
   {
     try
     {
       localv4 = InetAddress.getByName("127.0.0.1");
       localv6 = InetAddress.getByName("::1");
     }
     catch (UnknownHostException uhe)
     {
       throw new RuntimeException(uhe);
     }
   }
   
   public static boolean isLocalConnection(Socket socket)
   {
     InetAddress addr = socket.getInetAddress();
     if ((addr.equals(localv4)) || (addr.equals(localv6))) {
       return true;
     }
     try
     {
       if (addr.equals(InetAddress.getLocalHost())) {
         return true;
       }
     }
     catch (UnknownHostException e) {}
     return false;
   }
   
   public static String getLocalHostName()
   {
     String localMachineName;
     try
     {
       localMachineName = InetAddress.getLocalHost().getHostName();
     }
     catch (Exception ex)
     {
       localMachineName = "local";
     }
     return localMachineName;
   }
   
   public static boolean isLocalMachine(String serverName)
   {
     if ((serverName == null) || (serverName.isEmpty())) {
       return false;
     }
     if ((serverName.equalsIgnoreCase("localhost")) || (serverName.equalsIgnoreCase("local"))) {
       return true;
     }
     if (getLocalHostName().equalsIgnoreCase(serverName)) {
       return true;
     }
     try
     {
       InetAddress iname = InetAddress.getByName(serverName);
       if ((iname.equals(localv4)) || (iname.equals(localv6))) {
         return true;
       }
     }
     catch (UnknownHostException uhe) {}
     return false;
   }
   
   public static Socket localConnect(int port, int connectTimeout, int readTimeout)
     throws IOException
   {
     return connect(localv4, port, connectTimeout, readTimeout);
   }
   
   public static Socket connect(String hostName, int port, int connectTimeout, int readTimeout)
     throws IOException
   {
     if (isLocalMachine(hostName)) {
       return connect(localv4, port, connectTimeout, readTimeout);
     }
     SocketAddress sockAddr = new InetSocketAddress(hostName, port);
     return connectImpl(sockAddr, connectTimeout, readTimeout);
   }
   
   public static Socket connect(InetAddress hostAddr, int port, int connectTimeout, int readTimeout)
     throws IOException
   {
     SocketAddress sockAddr = new InetSocketAddress(hostAddr, port);
     return connectImpl(sockAddr, connectTimeout, readTimeout);
   }
   
   private static Socket connectImpl(SocketAddress sockAddr, int connectTimeout, int readTimeout)
     throws IOException
   {
     if (connectTimeout < 0) {
       connectTimeout = 0;
     }
     if (readTimeout < 0) {
       readTimeout = 0;
     }
     Socket sock = new Socket();
     sock.connect(sockAddr, connectTimeout);
     
     sock.setTcpNoDelay(true);
     sock.setSoTimeout(readTimeout);
     
     return sock;
   }
 }
