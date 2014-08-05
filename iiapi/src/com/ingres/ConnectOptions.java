/*  1:   */ package com.ingres;
/*  2:   */ 
/*  3:   */ import java.io.Serializable;
/*  4:   */ 
/*  5:   */ public final class ConnectOptions
/*  6:   */   implements Serializable
/*  7:   */ {
/*  8:   */   private static final long serialVersionUID = 3452130191923531265L;
/*  9:   */   public String effectiveUser;
/* 10:   */   public String group;
/* 11:   */   public String role;
/* 12:   */   public String dbmsPassword;
/* 13:   */   
/* 14:   */   public ConnectOptions(String effectiveUser, String group, String role, String dbmsPassword)
/* 15:   */   {
/* 16:29 */     this.effectiveUser = effectiveUser;
/* 17:30 */     this.group = group;
/* 18:31 */     this.role = role;
/* 19:32 */     this.dbmsPassword = dbmsPassword;
/* 20:   */   }
/* 21:   */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iiapi.jar
 * Qualified Name:     com.ingres.ConnectOptions
 * JD-Core Version:    0.7.0.1
 */