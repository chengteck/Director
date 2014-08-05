 package com.ingres;
 
 //import com.ingres.annotations.DefaultValue;
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class VerifyOptions
   implements Serializable
 {
   private static final long serialVersionUID = -6308838395016634387L;
   
   public static enum Mode
   {
     REPORT("report"),  RUN("run"),  RUNINTERACTIVE("runinteractive"),  RUNSILENT("runsilent");
     
     private String flagString;
     
     private Mode(String flagString)
     {
       this.flagString = flagString;
     }
     
     public String toString()
     {
       return this.flagString;
     }
   }
   
   public static enum Scope
   {
     DBNAME("dbname"),  DBA("dba"),  INSTALLATION("installation");
     
     private String flagString;
     
     private Scope(String flagString)
     {
       this.flagString = flagString;
     }
     
     public String toString()
     {
       return this.flagString;
     }
   }
   
   public static enum Operation
   {
     ACCESSCHECK("accesscheck"),  PURGE("purge"),  TEMP_PURGE("temp_purge"),  EXPIRED_PURGE("expired_purge"),  DROP_TABLE("drop_table"),  TABLE("table"),  XTABLE("xtable"),  DBMS_CATALOGS("dbms_catalogs"),  FORCE_CONSISTENT("force_consistent"),  REFRESH_LDBS("refresh_ldbs");
     
     private String flagString;
     
     private Operation(String flagString)
     {
       this.flagString = flagString;
     }
     
     public String toString()
     {
       return this.flagString;
     }
   }
   
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String effUser = null;
   //@Scriptable
   //@DefaultValue("REPORT")
   public Mode mode = Mode.REPORT;
   //@Scriptable
   //@DefaultValue("DBNAME")
   public Scope scope = Scope.DBNAME;
   //@Scriptable
   //@DefaultValue("ACCESSCHECK")
   public Operation operation = Operation.ACCESSCHECK;
   //@Scriptable
   public String[] dbnames = null;
   //@Scriptable
   public String[] tablenames = null;
   //@Scriptable
   public boolean noLog = false;
   //@Scriptable
   public String logfile = null;
   //@Scriptable
   public boolean verbose = false;
 }
