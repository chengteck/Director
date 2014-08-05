 package com.ingres;
 
 //import com.ingres.annotations.DefaultValue;
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class VWLoadOptions
   implements Serializable
 {
   static final long serialVersionUID = -6959245216540714595L;
   //@Scriptable
   public String cwd = null;
   //@Scriptable
   public String tablename = null;
   //@Scriptable
   public String[] filenames = null;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String attributes;
   //@Scriptable
   public String charset;
   //@Scriptable
   public String dateFormat;
   //@Scriptable
   public String escape;
   //@Scriptable
   //@DefaultValue("\\n")
   public String rdelim;
   //@Scriptable
   //@DefaultValue("|")
   public String fdelim;
   //@Scriptable
   public boolean header;
   //@Scriptable
   public boolean ignoreFirst;
   //@Scriptable
   public boolean ignoreLast;
   //@Scriptable
   public String log;
   //@Scriptable
   public String nullValue;
   //@Scriptable
   public String profile;
   //@Scriptable
   public String quote;
   //@Scriptable
   public String substitute;
   //@Scriptable
   public boolean rollback;
   //@Scriptable
   public int skip;
   //@Scriptable
   public String table;
   //@Scriptable
   public String effUser;
   //@Scriptable
   public boolean verbose;
   //@Scriptable
   public int errCount;
 }
