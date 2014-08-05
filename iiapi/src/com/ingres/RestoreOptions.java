 package com.ingres;
 
 //import com.ingres.annotations.DefaultValue;
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class RestoreOptions
   implements Serializable
 {
   private static final long serialVersionUID = -5244990477912468019L;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String effUser = null;
   //@Scriptable
   public boolean useCheckpoint = true;
   //@Scriptable
   //@DefaultValue("-1")
   public int checkpointNumber = -1;
   //@Scriptable
   //@DefaultValue("true")
   public boolean useJournal = true;
   public String[] tapeDevices = null;
   //@Scriptable
   //@DefaultValue("-1")
   public int locationsAtOnce = -1;
   //@Scriptable
   public boolean verbose = false;
   //@Scriptable
   public boolean wait = false;
   //@Scriptable
   public String beginDate = null;
   //@Scriptable
   public String endDate = null;
   //@Scriptable
   public boolean incremental = false;
   //@Scriptable
   public boolean noRollback = false;
   //@Scriptable
   public String[] tableNames = null;
   //@Scriptable
   public boolean noSecondaryIndex = false;
   //@Scriptable
   public boolean forceJournaling = false;
   //@Scriptable
   public boolean printStatistics = false;
   //@Scriptable
   public boolean ignoreErrors = false;
   //@Scriptable
   public boolean continueOnError = false;
   //@Scriptable
   public boolean promptOnError = false;
   //@Scriptable
   public boolean relocate = false;
   //@Scriptable
   public String[] oldLocations = null;
   //@Scriptable
   public String[] newLocations = null;
   //@Scriptable
   //@DefaultValue("-1")
   public int dmfCacheSize = -1;
   //@Scriptable
   //@DefaultValue("-1")
   public int dmfCacheSize4k = -1;
   //@Scriptable
   //@DefaultValue("-1")
   public int dmfCacheSize8k = -1;
   //@Scriptable
   //@DefaultValue("-1")
   public int dmfCacheSize16k = -1;
   //@Scriptable
   //@DefaultValue("-1")
   public int dmfCacheSize32k = -1;
   //@Scriptable
   //@DefaultValue("-1")
   public int dmfCacheSize64k = -1;
 }
