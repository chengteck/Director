/*  1:   */ package com.ingres.server;
/*  2:   */ 
/*  3:   */ import com.ingres.util.Intl;
/*  4:   */ 
/*  5:   */ public class ParameterException
/*  6:   */   extends RuntimeException
/*  7:   */ {
/*  8:   */   public ParameterException() {}
/*  9:   */   
/* 10:   */   public ParameterException(String name)
/* 11:   */   {
/* 12:44 */     super(Intl.formatString(RemoteCommand.pkg, "exception.badParameter", new Object[] { name }));
/* 13:   */   }
/* 14:   */   
/* 15:   */   public ParameterException(String name, String msg)
/* 16:   */   {
/* 17:50 */     super(Intl.formatString(RemoteCommand.pkg, "exception.paramError", new Object[] { name, msg }));
/* 18:   */   }
/* 19:   */   
/* 20:   */   public ParameterException(Throwable cause)
/* 21:   */   {
/* 22:55 */     super(cause);
/* 23:   */   }
/* 24:   */   
/* 25:   */   public ParameterException(String msg, Throwable cause)
/* 26:   */   {
/* 27:60 */     super(msg, cause);
/* 28:   */   }
/* 29:   */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iimgmtsvr.jar
 * Qualified Name:     com.ingres.server.ParameterException
 * JD-Core Version:    0.7.0.1
 */