 package com.ingres;
 
 import com.ingres.util.Environment;
 
 public class SessionAuth
 {
   private AuthType authType;
   private boolean notFullyTrusted;
   
   public static enum AuthType
   {
     PROCESS_UID,  CLIENT_UID,  CLIENT_PWD;
     
     private AuthType() {}
   }
   
   public static boolean isProcessUID(String uid)
   {
     return uid.equalsIgnoreCase(Environment.currentUser());
   }
   
   public static SessionAuth getAuth()
   {
     return new SessionAuth();
   }
   
   public static SessionAuth getLimitedAuth()
   {
     SessionAuth limitedAuth = new SessionAuth();
     limitedAuth.notFullyTrusted = true;
     return limitedAuth;
   }
   
   public static SessionAuth getAuth(String uid)
   {
     return new ClientAuth(uid);
   }
   
   public static SessionAuth getAuth(String uid, byte[] pwd)
   {
     return new PasswordAuth(uid, pwd);
   }
   
   public static SessionAuth getAuth(String uid, byte[] pwd, byte[] dbmsPwd)
   {
     return new PasswordAuth(uid, pwd, dbmsPwd);
   }
   
   public AuthType getAuthType()
   {
     return this.authType;
   }
   
   public String getUID()
   {
     return Environment.currentUser();
   }
   
   public byte[] getPWD()
   {
     return null;
   }
   
   public byte[] getDBMSPWD()
   {
     return null;
   }
   
   public boolean isNotFullyTrusted()
   {
     return this.notFullyTrusted;
   }
   
   private SessionAuth()
   {
     this.authType = AuthType.PROCESS_UID;
   }
   
   private SessionAuth(AuthType type)
   {
     this.authType = type;
   }
   
   private static class ClientAuth
     extends SessionAuth
   {
     private String uid;
     
     public ClientAuth(String uid)
     {
       super(null);
       this.uid = uid;
     }
     
     public String getUID()
     {
       return this.uid;
     }
   }
   
   private static class PasswordAuth
     extends SessionAuth
   {
     private String uid;
     private byte[] pwd;
     private byte[] dbmsPwd;
     
     public PasswordAuth(String uid, byte[] pwd)
     {
       super(null);
       this.uid = uid;
       this.pwd = pwd;
       this.dbmsPwd = null;
     }
     
     public PasswordAuth(String uid, byte[] pwd, byte[] dbmsPwd)
     {
       super(null);
       this.uid = uid;
       this.pwd = pwd;
       this.dbmsPwd = dbmsPwd;
     }
     
     public String getUID()
     {
       return this.uid;
     }
     
     public byte[] getPWD()
     {
       return this.pwd;
     }
     
     public byte[] getDBMSPWD()
     {
       return this.dbmsPwd;
     }
   }
 }
