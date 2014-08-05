 package com.ingres;
 
 //import com.ingres.annotations.Scriptable;
 import com.ingres.util.NumericUtil;
 import java.io.Serializable;
 import java.util.EnumSet;
 
 public final class CreateDBOptions
   implements Serializable
 {
   private static final long serialVersionUID = 8550620432586539425L;
   //@Scriptable
   public ServerClass serverClass = null;
   //@Scriptable
   public String cdbname = null;
   //@Scriptable
   public String databaseLoc = null; //数据库文件存储路径
   //@Scriptable
   public String checkpointLoc = null;//检查点文件存储路径
   //@Scriptable
   public String journalLoc = null;//归档文件存储路径
   //@Scriptable
   public String dumpLoc = null;//备份文件存储路径
   //@Scriptable
   public String workLoc = null;
   public EnumSet<CatalogProduct> catalogProducts = null;
   //@Scriptable
   public boolean unicode = true;
   //@Scriptable
   public int normalization = 0;
   public static final int NORMAL_NFC = 0;
   public static final int NORMAL_NFD = 1;
   //@Scriptable
   public String collationName = null;
   //@Scriptable
   public boolean privateDB = false;
   //@Scriptable
   public String effUser = null;
   //@Scriptable
   public String password = null;
   //@Scriptable
   public String readOnlyLoc = null;
   //@Scriptable
   public int pageSize = 0;
   public static final int PAGESIZE_DEFAULT = 0;
   //@Scriptable
   public boolean alwaysLogged = false;
   
   public static int convertPageSize(String size)
   {
     if (size.equalsIgnoreCase("default")) {
       return 0;
     }
     return (int)NumericUtil.convertKMGValue(size);
   }
   
   public static int convertNormalization(String value)
   {
     if (value.equalsIgnoreCase("NFC")) {
       return 0;
     }
     if (value.equalsIgnoreCase("NFD")) {
       return 1;
     }
     throw new IllegalArgumentException(String.format("Value '%1$s' is not a recognized normalization.", new Object[] { value }));
   }
 }