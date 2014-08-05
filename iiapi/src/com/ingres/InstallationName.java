/*   1:    */ package com.ingres;
/*   2:    */ 
/*   3:    */ import com.ingres.util.NetworkUtil;
/*   4:    */ import java.util.regex.Matcher;
/*   5:    */ import java.util.regex.Pattern;
/*   6:    */ 
/*   7:    */ public final class InstallationName
/*   8:    */ {
/*   9: 65 */   private String serverName = null;
/*  10: 66 */   private String installationCode = null;
/*  11: 68 */   private static Pattern installationCodePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]$");
/*  12:    */   
/*  13:    */   public boolean isFullyQualified()
/*  14:    */   {
/*  15: 79 */     return (this.installationCode != null) && (!this.installationCode.isEmpty());
/*  16:    */   }
/*  17:    */   
/*  18:    */   public String toString()
/*  19:    */   {
/*  20: 84 */     return isFullyQualified() ? String.format("%s/%s", new Object[] { getServerName(), getInstallationCode() }) : getServerName();
/*  21:    */   }
/*  22:    */   
/*  23:    */   public String getServerName()
/*  24:    */   {
/*  25: 88 */     return this.serverName;
/*  26:    */   }
/*  27:    */   
/*  28:    */   public void setServerName(String serverName)
/*  29:    */   {
/*  30: 92 */     this.serverName = serverName.trim();
/*  31:    */   }
/*  32:    */   
/*  33:    */   public String getInstallationCode()
/*  34:    */   {
/*  35: 96 */     return this.installationCode;
/*  36:    */   }
/*  37:    */   
/*  38:    */   public void setInstallationCode(String installationCode)
/*  39:    */   {
/*  40:100 */     if (installationCode == null) {
/*  41:101 */       throw new NullPointerException("Installation code is null");
/*  42:    */     }
/*  43:103 */     installationCode = installationCode.trim();
/*  44:104 */     if (installationCode.isEmpty()) {
/*  45:105 */       throw new IllegalArgumentException("Installation code is empty");
/*  46:    */     }
/*  47:107 */     Matcher m = installationCodePattern.matcher(installationCode);
/*  48:108 */     if (!m.matches()) {
/*  49:109 */       throw new IllegalArgumentException("Invalid form of installation code: must be two letters or a letter and a digit.");
/*  50:    */     }
/*  51:111 */     this.installationCode = installationCode.toUpperCase();
/*  52:    */   }
/*  53:    */   
/*  54:    */   public InstallationName(String name)
/*  55:    */   {
/*  56:122 */     if (name == null) {
/*  57:123 */       throw new NullPointerException("Installation name is null");
/*  58:    */     }
/*  59:125 */     name = name.trim();
/*  60:126 */     if (name.isEmpty()) {
/*  61:127 */       throw new IllegalArgumentException("Installation name is empty");
/*  62:    */     }
/*  63:129 */     String[] parts = name.split("[\\\\/;]");
/*  64:130 */     if (parts.length > 2) {
/*  65:131 */       throw new IllegalArgumentException("Installation name is wrong format");
/*  66:    */     }
/*  67:135 */     if (parts.length == 1)
/*  68:    */     {
/*  69:    */       try
/*  70:    */       {
/*  71:137 */         setInstallationCode(parts[0]);
/*  72:138 */         setServerName(NetworkUtil.getLocalHostName());
/*  73:    */       }
/*  74:    */       catch (IllegalArgumentException iae)
/*  75:    */       {
/*  76:143 */         setServerName(parts[0]);
/*  77:    */       }
/*  78:    */     }
/*  79:    */     else
/*  80:    */     {
/*  81:147 */       setServerName(parts[0]);
/*  82:148 */       setInstallationCode(parts[1]);
/*  83:    */     }
/*  84:    */   }
/*  85:    */   
/*  86:    */   public InstallationName(String server, String installationCode)
/*  87:    */   {
/*  88:154 */     setServerName(server);
/*  89:155 */     setInstallationCode(installationCode);
/*  90:    */   }
/*  91:    */   
/*  92:    */   public boolean isLocalMachine()
/*  93:    */   {
/*  94:164 */     if ((this.serverName == null) || (this.serverName.isEmpty())) {
/*  95:165 */       return true;
/*  96:    */     }
/*  97:167 */     return NetworkUtil.isLocalMachine(this.serverName);
/*  98:    */   }
/*  99:    */   
/* 100:    */   public String getMachineName()
/* 101:    */   {
/* 102:175 */     if (isLocalMachine()) {
/* 103:176 */       return "localhost";
/* 104:    */     }
/* 105:177 */     return this.serverName;
/* 106:    */   }
/* 107:    */   
/* 108:    */   public String getDisplayName(boolean local, boolean single)
/* 109:    */   {
/* 110:182 */     return local ? "local" : isFullyQualified() ? String.format("%1$s/%2$s", new Object[] { "local", this.installationCode }) : single ? "local" : toString();
/* 111:    */   }
/* 112:    */   
/* 113:    */   public boolean equals(Object o)
/* 114:    */   {
/* 115:190 */     boolean equal = false;
/* 116:191 */     if ((o instanceof InstallationName))
/* 117:    */     {
/* 118:192 */       InstallationName iName = (InstallationName)o;
/* 119:193 */       equal = ((this.serverName == null) && (iName.serverName == null)) || ((this.serverName != null) && (this.serverName.equalsIgnoreCase(iName.serverName)) && (((this.installationCode == null) && (iName.installationCode == null)) || ((this.installationCode != null) && (this.installationCode.equalsIgnoreCase(iName.installationCode)))));
/* 120:    */     }
/* 121:198 */     return equal;
/* 122:    */   }
/* 123:    */   
/* 124:    */   public int hashCode()
/* 125:    */   {
/* 126:203 */     return (this.serverName == null ? 0 : this.serverName.toLowerCase().hashCode()) ^ (this.installationCode == null ? 0 : this.installationCode.toLowerCase().hashCode());
/* 127:    */   }
/* 128:    */ }


/* Location:           C:\Program Files (x86)\Ingres\IngresII\ingres\lib\iiapi.jar
 * Qualified Name:     com.ingres.InstallationName
 * JD-Core Version:    0.7.0.1
 */