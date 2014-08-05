/*   1:    */ package com.ingres;
/*   2:    */ 
/*   3:    */ import com.ingres.util.Intl;
/*   4:    */ import com.ingres.util.Logging;
/*   5:    */ 
/*   6:    */ public final class UtilityOutputHandler
/*   7:    */ {
/*   8:    */   private boolean blocking;
/*   9:    */   private TaskWrapper task;
/*  10:    */   private LogTextCallback logTextCallback;
/*  11:    */   private Runnable progressCallback;
/*  12:    */   private int maxReadLength;
/*  13:    */   private int pollDelay;
/*  14:    */   private int emptyReadLimit;
/*  15:    */   private IIapi inst;
/*  16:    */   private UtilityProcessHandle processHandle;
/*  17:    */   private boolean showStartEndLogging;
/*  18:    */   private String utilityDisplayName;
/*  19:    */   
/*  20:    */   public static class Options
/*  21:    */   {
/*  22:102 */     public boolean blocking = true;
/*  23:108 */     public UtilityOutputHandler.TaskWrapper task = new UtilityOutputHandler.TaskWrapper()
/*  24:    */     {
/*  25:    */       public boolean isAborted()
/*  26:    */       {
/*  27:111 */         return false;
/*  28:    */       }
/*  29:    */       
/*  30:    */       public void abort() {}
/*  31:    */     };
/*  32:121 */     public UtilityOutputHandler.LogTextCallback logTextCallback = null;
/*  33:126 */     public Runnable progressCallback = null;
/*  34:132 */     public int maxReadLength = 8200;
/*  35:140 */     public int pollDelay = 200;
/*  36:146 */     public int emptyReadLimit = 1;
/*  37:    */   }
/*  38:    */   
/*  39:    */   public UtilityOutputHandler(IIapi inst, UtilityProcessHandle processHandle, LogTextCallback logTextCallback, Options options, boolean showStartEndLogging, String utilityDisplayName)
/*  40:    */   {
/*  41:247 */     this.inst = inst;
/*  42:248 */     this.processHandle = processHandle;
/*  43:249 */     this.logTextCallback = logTextCallback;
/*  44:250 */     this.blocking = options.blocking;
/*  45:251 */     this.task = options.task;
/*  46:252 */     this.progressCallback = options.progressCallback;
/*  47:253 */     this.maxReadLength = options.maxReadLength;
/*  48:254 */     this.pollDelay = options.pollDelay;
/*  49:255 */     this.emptyReadLimit = options.emptyReadLimit;
/*  50:256 */     this.showStartEndLogging = showStartEndLogging;
/*  51:257 */     this.utilityDisplayName = utilityDisplayName;
/*  52:    */   }
/*  53:    */   
/*  54:    */   public UtilityOutputHandler(IIapi inst, UtilityProcessHandle processHandle, LogTextCallback logTextCallback, Options options)
/*  55:    */   {
/*  56:269 */     this(inst, processHandle, logTextCallback, options, false, null);
/*  57:    */   }
/*  58:    */   
/*  59:    */   public int showProcessOutput()
/*  60:    */   {
/*  61:    */     try
/*  62:    */     {
/*  63:286 */       if ((this.showStartEndLogging) && (this.logTextCallback != null)) {
/*  64:287 */         this.logTextCallback.displayNextText(Intl.formatString(IIapi.pkg, "utilityOutput.startMessage", new Object[] { this.utilityDisplayName }));
/*  65:    */       }
/*  66:290 */       int emptyReads = 0;
/*  67:    */       
/*  68:292 */       String nextText = readNext();
/*  69:294 */       while (nextText != null)
/*  70:    */       {
/*  71:295 */         if ((nextText.length() > 0) && 
/*  72:296 */           (this.logTextCallback != null)) {
/*  73:297 */           this.logTextCallback.displayNextText(nextText);
/*  74:    */         }
/*  75:299 */         if (this.progressCallback != null) {
/*  76:300 */           this.progressCallback.run();
/*  77:    */         }
/*  78:302 */         sleep(this.pollDelay);
/*  79:    */         
/*  80:304 */         nextText = readNext();
/*  81:306 */         if (!this.blocking) {
/*  82:310 */           if ((nextText != null) && (nextText.length() == 0))
/*  83:    */           {
/*  84:312 */             emptyReads++;
/*  85:314 */             if (emptyReads > this.emptyReadLimit)
/*  86:    */             {
/*  87:316 */               Logging.Debug("UtilityOutputHandler: Checking ProcessExitValue because emptyReadLimit exceeded", new Object[0]);
/*  88:318 */               if (getProcessExitValue() != 2147483647)
/*  89:    */               {
/*  90:320 */                 Logging.Debug("UtilityOutputHandler: Process completed, without EOF.  Checking for final output", new Object[0]);
/*  91:    */                 
/*  92:322 */                 nextText = readNext();
/*  93:324 */                 if ((nextText != null) && (nextText.length() == 0))
/*  94:    */                 {
/*  95:325 */                   Logging.Debug("UtilityOutputHandler: Still no output.  Now simulating EOF", new Object[0]);
/*  96:326 */                   break;
/*  97:    */                 }
/*  98:331 */                 Logging.Debug("UtilityOutputHandler: Got some output.  Continuing to display and poll", new Object[0]);
/*  99:332 */                 this.emptyReadLimit = 0;
/* 100:    */               }
/* 101:    */               else
/* 102:    */               {
/* 103:337 */                 emptyReads = 0;
/* 104:    */               }
/* 105:    */             }
/* 106:    */           }
/* 107:    */           else
/* 108:    */           {
/* 109:343 */             emptyReads = 0;
/* 110:    */           }
/* 111:    */         }
/* 112:    */       }
/* 113:348 */       int exitValue = waitForProcessExitValue();
/* 114:349 */       Logging.Debug("UtilityOutputHandler: Exit Value = %x", new Object[] { Integer.valueOf(exitValue) });
/* 115:352 */       if ((this.showStartEndLogging) && (this.logTextCallback != null)) {
/* 116:353 */         this.logTextCallback.displayNextText(Intl.formatString(IIapi.pkg, "utilityOutput.completionMessage", new Object[0]));
/* 117:    */       }
/* 118:355 */       return exitValue;
/* 119:    */     }
/* 120:    */     finally
/* 121:    */     {
/* 122:    */       try
/* 123:    */       {
/* 124:361 */         this.inst.UTreleaseProcessHandle(this.processHandle);
/* 125:    */       }
/* 126:    */       catch (Exception ignore) {}
/* 127:    */     }
/* 128:    */   }
/* 129:    */   
/* 130:    */   private void checkForAbort()
/* 131:    */   {
/* 132:376 */     if (this.task.isAborted())
/* 133:    */     {
/* 134:377 */       Logging.Debug("UtilityOutputHandler: ABORTING", new Object[0]);
/* 135:378 */       this.task.abort();
/* 136:    */     }
/* 137:    */   }
/* 138:    */   
/* 139:    */   private void translateExceptions(IIapi.Exception iae)
/* 140:    */   {
/* 141:402 */     checkForAbort();
/* 142:403 */     throw iae;
/* 143:    */   }
/* 144:    */   
/* 145:    */   private String readNext()
/* 146:    */   {
/* 147:411 */     String nextText = null;
/* 148:    */     try
/* 149:    */     {
/* 150:413 */       checkForAbort();
/* 151:414 */       nextText = this.inst.UTreadProcessOutput(this.processHandle, this.maxReadLength, this.blocking);
/* 152:    */     }
/* 153:    */     catch (IIapi.Exception iae)
/* 154:    */     {
/* 155:417 */       translateExceptions(iae);
/* 156:    */     }
/* 157:420 */     if (nextText != null) {
/* 158:421 */       Logging.Debug("UtilityOutputHandler: nextText.length() = %d", new Object[] { Integer.valueOf(nextText.length()) });
/* 159:    */     } else {
/* 160:423 */       Logging.Debug("UtilityOutputHandler: GOT EOF", new Object[0]);
/* 161:    */     }
/* 162:425 */     return nextText;
/* 163:    */   }
/* 164:    */   
/* 165:    */   private int getProcessExitValue()
/* 166:    */   {
/* 167:433 */     int exitValue = -2147483648;
/* 168:    */     try
/* 169:    */     {
/* 170:435 */       checkForAbort();
/* 171:436 */       exitValue = this.inst.UTgetProcessExitValue(this.processHandle);
/* 172:    */     }
/* 173:    */     catch (IIapi.Exception iae)
/* 174:    */     {
/* 175:439 */       translateExceptions(iae);
/* 176:    */     }
/* 177:441 */     return exitValue;
/* 178:    */   }
/* 179:    */   
/* 180:    */   private int waitForProcessExitValue()
/* 181:    */   {
/* 182:449 */     int exitValue = -2147483648;
/* 183:    */     try
/* 184:    */     {
/* 185:451 */       checkForAbort();
/* 186:452 */       exitValue = this.inst.UTwaitForProcessExitValue(this.processHandle);
/* 187:    */     }
/* 188:    */     catch (IIapi.Exception iae)
/* 189:    */     {
/* 190:455 */       translateExceptions(iae);
/* 191:    */     }
/* 192:457 */     return exitValue;
/* 193:    */   }
/* 194:    */   
/* 195:    */   private void sleep(int pollDelay)
/* 196:    */   {
/* 197:    */     try
/* 198:    */     {
/* 199:470 */       Thread.sleep(pollDelay);
/* 200:    */     }
/* 201:    */     catch (InterruptedException ie)
/* 202:    */     {
/* 203:473 */       this.task.abort();
/* 204:    */     }
/* 205:    */   }
/* 206:    */   
/* 207:    */   public static abstract interface LogTextCallback
/* 208:    */   {
/* 209:    */     public abstract void displayNextText(String paramString);
/* 210:    */   }
/* 211:    */   
/* 212:    */   public static abstract interface TaskWrapper
/* 213:    */   {
/* 214:    */     public abstract boolean isAborted();
/* 215:    */     
/* 216:    */     public abstract void abort();
/* 217:    */   }
/* 218:    */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iiapi.jar
 * Qualified Name:     com.ingres.UtilityOutputHandler
 * JD-Core Version:    0.7.0.1
 */