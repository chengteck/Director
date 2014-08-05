/*   1:    */ package com.ingres;
/*   2:    */ 
/*   3:    */ import java.io.Serializable;
/*   4:    */ import java.util.EnumSet;
/*   5:    */ 
/*   6:    */ public final class ExtendOptions
/*   7:    */   implements Serializable
/*   8:    */ {
/*   9:    */   private static final long serialVersionUID = 7461386821078146138L;
/*  10:    */   
/*  11:    */   public static enum Usage
/*  12:    */   {
/*  13: 31 */     DATA("data"),  CHECKPOINT("ckp"),  JOURNAL("jnl"),  DUMP("dmp"),  WORK("work"),  AUXILIARY_WORK("awork");
/*  14:    */     
/*  15:    */     private String flagString;
/*  16:    */     
/*  17:    */     private Usage(String flagString)
/*  18:    */     {
/*  19: 41 */       this.flagString = flagString;
/*  20:    */     }
/*  21:    */     
/*  22:    */     public String toString()
/*  23:    */     {
/*  24: 46 */       return this.flagString;
/*  25:    */     }
/*  26:    */   }
/*  27:    */   
/*  28: 53 */   public String cwd = null;
/*  29: 59 */   public ServerClass serverClass = null;
/*  30: 65 */   public String effUser = null;
/*  31: 71 */   public String location = null;
/*  32: 77 */   public boolean noDB = false;
/*  33: 83 */   public String[] dbnames = null;
/*  34: 89 */   public String areaDirectory = null;
/*  35: 95 */   public EnumSet<Usage> locationUsages = null;
/*  36:100 */   public int rawPct = -1;
/*  37:105 */   public boolean drop = false;
/*  38:110 */   public boolean alter = false;
/*  39:    */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iiapi.jar
 * Qualified Name:     com.ingres.ExtendOptions
 * JD-Core Version:    0.7.0.1
 */