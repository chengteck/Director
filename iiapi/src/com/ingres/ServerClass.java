/*  1:   */ package com.ingres;
/*  2:   */ 
/*  3:   */ public enum ServerClass
/*  4:   */ {
/*  5:26 */   INGRES_DBMS("Ingres DBMS", "ingres"),  INGRES_STAR("Ingres Star", "star"),  DB2("DB2", "db2"),  DB2_UDB("DB2 UDB", "db2udb"),  IMS("IMS", "ims"),  RMS("RMS", "rms"),  VSAM("VSAM", "vsam"),  MS_SQLSERVER("MS SQL Server", "mssql"),  ORACLE("Oracle", "oracle");
/*  6:   */   
/*  7:   */   private String name;
/*  8:   */   private String serverClass;
/*  9:39 */   private static ServerClass[] dbServerClasses = { INGRES_DBMS, INGRES_STAR };
/* 10:   */   
/* 11:   */   private ServerClass(String name, String serverClass)
/* 12:   */   {
/* 13:45 */     this.name = name;
/* 14:46 */     this.serverClass = serverClass;
/* 15:   */   }
/* 16:   */   
/* 17:   */   public String toString()
/* 18:   */   {
/* 19:51 */     return this.name;
/* 20:   */   }
/* 21:   */   
/* 22:   */   public String getName()
/* 23:   */   {
/* 24:55 */     return this.name;
/* 25:   */   }
/* 26:   */   
/* 27:   */   public String getServerClass()
/* 28:   */   {
/* 29:59 */     return this.serverClass;
/* 30:   */   }
/* 31:   */   
/* 32:   */   public static ServerClass[] getDBServerClasses()
/* 33:   */   {
/* 34:63 */     return dbServerClasses;
/* 35:   */   }
/* 36:   */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iiapi.jar
 * Qualified Name:     com.ingres.ServerClass
 * JD-Core Version:    0.7.0.1
 */