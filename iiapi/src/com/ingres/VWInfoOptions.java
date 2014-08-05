 package com.ingres;
 
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class VWInfoOptions
   implements Serializable
 {
   private static final long serialVersionUID = -7409574957210700334L;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String effUser = null;
   public boolean launchIIvwinfo = false;
   //@Scriptable
   public boolean stats = false;
   //@Scriptable
   public boolean config = false;
   //@Scriptable
   public boolean tableBlockUse = false;
   //@Scriptable
   public boolean columnBlockUse = false;
   //@Scriptable
   public boolean openTransactions = false;
   //@Scriptable
   public String tablename = null;
   //@Scriptable
   public boolean verbose = false;
 }
