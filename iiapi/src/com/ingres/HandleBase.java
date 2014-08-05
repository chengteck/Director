/*  1:   */ package com.ingres;
/*  2:   */ 
/*  3:   */ import java.io.Serializable;
/*  4:   */ 
/*  5:   */ public abstract class HandleBase
/*  6:   */   implements Serializable
/*  7:   */ {
/*  8:   */   private static final long serialVersionUID = 7490585162678971197L;
/*  9:   */   private long handleValue;
/* 10:   */   
/* 11:   */   protected HandleBase() {}
/* 12:   */   
/* 13:   */   HandleBase(long handleValue)
/* 14:   */   {
/* 15:49 */     this.handleValue = handleValue;
/* 16:   */   }
/* 17:   */   
/* 18:   */   public boolean equals(Object obj)
/* 19:   */   {
/* 20:54 */     return (obj != null) && (getClass() == obj.getClass()) && (this.handleValue == ((HandleBase)obj).handleValue);
/* 21:   */   }
/* 22:   */   
/* 23:   */   public int hashCode()
/* 24:   */   {
/* 25:60 */     return (int)this.handleValue;
/* 26:   */   }
/* 27:   */   
/* 28:   */   public String toString()
/* 29:   */   {
/* 30:65 */     return String.format("0x%1$x", new Object[] { Long.valueOf(this.handleValue) });
/* 31:   */   }
/* 32:   */   
/* 33:   */   public void update(HandleBase newHandle)
/* 34:   */   {
/* 35:69 */     this.handleValue = newHandle.handleValue;
/* 36:   */   }
/* 37:   */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iiapi.jar
 * Qualified Name:     com.ingres.HandleBase
 * JD-Core Version:    0.7.0.1
 */