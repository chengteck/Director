 package com.ingres;
 
 //import com.ingres.annotations.DefaultValue;
 //import com.ingres.annotations.Scriptable;
 import java.io.Serializable;
 import java.util.EnumSet;
 
 public final class SysmodOptions
   implements Serializable
 {
   private static final long serialVersionUID = -5905295011217448434L;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String[] tables = null;
   public EnumSet<CatalogProduct> catalogProducts = null;
   //@Scriptable
   //@DefaultValue("-1")
   public int newPageSize = -1;
   //@Scriptable
   public boolean wait = false;
 }
