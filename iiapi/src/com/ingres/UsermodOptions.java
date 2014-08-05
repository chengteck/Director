 package com.ingres;
 
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class UsermodOptions
   implements Serializable
 {
   private static final long serialVersionUID = 7711228253364216450L;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String effUser = null;
   //@Scriptable
   public String[] tables = null;
   //@Scriptable
   public boolean online = false;
   //@Scriptable
   public boolean noint = false;
   //@Scriptable
   public boolean repmod = false;
   //@Scriptable
   public boolean repmodWait = false;
 }
