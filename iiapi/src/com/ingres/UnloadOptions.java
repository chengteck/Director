 package com.ingres;
 
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class UnloadOptions
   implements Serializable
 {
   private static final long serialVersionUID = -9034766640105937525L;
   //@Scriptable
   public String cwd = null;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public boolean printableData = false;
   //@Scriptable
   public String outputDirectory = null;
   //@Scriptable
   public String source = null;
   //@Scriptable
   public String dest = null;
   public boolean promptForPassword = false;
   //@Scriptable
   public String effUser = null;
   //@Scriptable
   public String groupID = null;
   //@Scriptable
   public boolean parallel = false;
   //@Scriptable
   public boolean journal = false;
   //@Scriptable
   public boolean withSequences = false;
   //@Scriptable
   public boolean groupTableIndexes = false;
   //@Scriptable
   public boolean noRep = false;
   //@Scriptable
   public boolean noLogging = false;
 }