 package com.ingres;
 
 //import com.ingres.annotations.DefaultValue;
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 
 public final class DestroyOptions
   implements Serializable
 {
   private static final long serialVersionUID = -7416451439386178406L;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
  // @DefaultValue("true")
   public boolean abortIfInUse = true;
  // @Scriptable
   public String effUser = null;
   //@Scriptable
   public String password = null;
 }


