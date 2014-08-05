 package com.ingres.server;
 
 import com.ingres.AlterOptions;
import com.ingres.AuditOptions;
import com.ingres.BackupOptions;
import com.ingres.ConnectOptions;
import com.ingres.ConnectionHandle;
import com.ingres.ContextHandle;
import com.ingres.CopyOptions;
import com.ingres.CreateDBOptions;
import com.ingres.DestroyOptions;
import com.ingres.ExtendOptions;
import com.ingres.FileHandle;
import com.ingres.HandleBase;
import com.ingres.IIapi;
import com.ingres.IIapi.DataValue;
import com.ingres.IIapi.Descriptor;
import com.ingres.IIapi.Exception;
import com.ingres.IIapi.Privileges;
import com.ingres.IIapi.QueryInfo;
import com.ingres.IIapi.SysInfo;
import com.ingres.IIapi.VersionInfo;
import com.ingres.InfoOptions;
import com.ingres.OptimizeOptions;
import com.ingres.RestoreOptions;
import com.ingres.SessionAuth;
import com.ingres.SessionAuth.AuthType;
import com.ingres.StartOptions;
import com.ingres.StatdumpOptions;
import com.ingres.StatementHandle;
import com.ingres.StopOptions;
import com.ingres.SysmodOptions;
import com.ingres.TransactionHandle;
import com.ingres.UnloadOptions;
import com.ingres.UsermodOptions;
import com.ingres.UtilityProcessHandle;
import com.ingres.VWInfoOptions;
import com.ingres.VWLoadOptions;
import com.ingres.VerifyOptions;
import com.ingres.util.Intl;
import com.ingres.util.Logging;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.Map;
 
 public class Parameter
   implements Cloneable
 {
   private String name;
   private int ordinal;
   private Class<?> type; // This is What?!
   public IO dir;
   private Object value;
   
   public static enum IO
   {
     INPUT,  OUTPUT,  INOUT;
     
     private IO() {}
   }
   
   public static final Class<?> BYTE_ARRAY_CLASS = java.lang.Byte.class;
   public static final Class<?> STRING_ARRAY_CLASS = java.lang.String.class;
   public static final Class<?> DESC_ARRAY_CLASS = com.ingres.IIapi.Descriptor.class;
   public static final Class<?> DATA_ARRAY_CLASS = com.ingres.IIapi.DataValue.class;
   public static final Class<?> DATA_2D_ARRAY_CLASS = com.ingres.IIapi.DataValue.class;
   
   public Parameter(int ord, Class<?> t, String n, IO in)
   {
     this.name = n;
     this.ordinal = ord;
     this.type = t;
     this.dir = in;
     this.value = null;
   }
   
   public Parameter(int ord, Class<?> t, String n)
   {
     this(ord, t, n, IO.INPUT);
   }
   
   public Parameter(Class<?> t)
   {
     this.name = "return";
     this.ordinal = -1;
     this.type = t;
     this.dir = IO.OUTPUT;
     this.value = null;
   }
   
   public Parameter(int ord, Class<?> t, IO in, Object val)
   {
     this.name = String.format("Parameter %1$d", new Object[] { Integer.valueOf(ord) });
     this.ordinal = ord;
     this.type = t;
     this.dir = in;
     this.value = val;
   }
   
   public String getShortName()
   {
     return String.format("'%1$s'", new Object[] { this.name });
   }
   
   public String getLongName()
   {
     return String.format("(#%1$d: %2$s '%3$s')", new Object[] { Integer.valueOf(this.ordinal), this.type.getName(), this.name });
   }
   
   public String toString()
   {
     return String.format("%1$s = '%2$s'", new Object[] { getLongName(), this.value });
   }
   
   public boolean check(Parameter actual)
   {
     if ((this.type != actual.type) || (this.ordinal != actual.ordinal) || (this.dir != actual.dir)) {
       return false;
     }
     actual.name = this.name;
     return true;
   }
   
   public <T> T getObjectValue(Class<T> type)
   {
     if (this.type == type)
     {
       if (this.value == null) {
         return null;
       }
       if (this.type.isInstance(this.value)) {
         return type.cast(this.value);
       }
     }
     throw new ParameterException(toString(), Intl.formatString(RemoteCommand.pkg, "param.cannotGetValue", new Object[] { type.getName() }));
   }
   
   public int getIntValue()
   {
     return ((Integer)getObjectValue(Integer.class)).intValue();
   }
   
   public long getLongValue()
   {
     return ((Long)getObjectValue(Long.class)).longValue();
   }
   
   public boolean getBooleanValue()
   {
     return ((Boolean)getObjectValue(Boolean.class)).booleanValue();
   }
   
   public ConnectionHandle getConnectionHandleValue()
   {
     return (ConnectionHandle)getObjectValue(ConnectionHandle.class);
   }
   
   public ContextHandle getContextHandleValue()
   {
     return (ContextHandle)getObjectValue(ContextHandle.class);
   }
   
   public StatementHandle getStatementHandleValue()
   {
     return (StatementHandle)getObjectValue(StatementHandle.class);
   }
   
   public TransactionHandle getTransactionHandleValue()
   {
     return (TransactionHandle)getObjectValue(TransactionHandle.class);
   }
   
   public UtilityProcessHandle getUtilityProcessHandleValue()
   {
     return (UtilityProcessHandle)getObjectValue(UtilityProcessHandle.class);
   }
   
   public FileHandle getFileHandleValue()
   {
     return (FileHandle)getObjectValue(FileHandle.class);
   }
   
   public String getStringValue()
   {
     return (String)getObjectValue(String.class);
   }
   
   public byte[] getByteArrayValue()
   {
     return (byte[])getObjectValue(BYTE_ARRAY_CLASS);
   }
   
   public String[] getStringArrayValue()
   {
     return (String[])getObjectValue(STRING_ARRAY_CLASS);
   }
   
   public IIapi.Descriptor[] getDescArrayValue()
   {
     return (IIapi.Descriptor[])getObjectValue(DESC_ARRAY_CLASS);
   }
   
   public IIapi.DataValue[] getDataArrayValue()
   {
     return (IIapi.DataValue[])getObjectValue(DATA_ARRAY_CLASS);
   }
   
   public IIapi.DataValue[][] getData2DArrayValue()
   {
     return (IIapi.DataValue[][])getObjectValue(DATA_2D_ARRAY_CLASS);
   }
   
   public IIapi.QueryInfo getQueryInfoValue()
   {
     return (IIapi.QueryInfo)getObjectValue(IIapi.QueryInfo.class);
   }
   
   public IIapi.VersionInfo getVersionInfoValue()
   {
     return (IIapi.VersionInfo)getObjectValue(IIapi.VersionInfo.class);
   }
   
   public IIapi.SysInfo getSysInfoValue()
   {
     return (IIapi.SysInfo)getObjectValue(IIapi.SysInfo.class);
   }
   
   public IIapi.Exception getIIapiExceptionValue()
   {
     return (IIapi.Exception)getObjectValue(IIapi.Exception.class);
   }
   
   public Map<String, String> getMapStringStringValue()
   {
     if ((this.type == Map.class) && ((this.value instanceof Map)))
     {
       Map<String, String> m = (Map)this.value;
       return m;
     }
     throw new ParameterException(toString(), Intl.getString(RemoteCommand.pkg, "param.cannotGetMap"));
   }
   
   public StartOptions getStartOptionsValue()
   {
     return (StartOptions)getObjectValue(StartOptions.class);
   }
   
   public StopOptions getStopOptionsValue()
   {
     return (StopOptions)getObjectValue(StopOptions.class);
   }
   
   public CreateDBOptions getCreateDBOptionsValue()
   {
     return (CreateDBOptions)getObjectValue(CreateDBOptions.class);
   }
   
   public DestroyOptions getDestroyOptionsValue()
   {
     return (DestroyOptions)getObjectValue(DestroyOptions.class);
   }
   
   public BackupOptions getBackupOptionsValue()
   {
     return (BackupOptions)getObjectValue(BackupOptions.class);
   }
   
   public RestoreOptions getRestoreOptionsValue()
   {
     return (RestoreOptions)getObjectValue(RestoreOptions.class);
   }
   
   public OptimizeOptions getOptimizeOptionsValue()
   {
     return (OptimizeOptions)getObjectValue(OptimizeOptions.class);
   }
   
   public SysmodOptions getSysmodOptionsValue()
   {
     return (SysmodOptions)getObjectValue(SysmodOptions.class);
   }
   
   public UsermodOptions getUsermodOptionsValue()
   {
     return (UsermodOptions)getObjectValue(UsermodOptions.class);
   }
   
   public VWLoadOptions getVWLoadOptionsValue()
   {
     return (VWLoadOptions)getObjectValue(VWLoadOptions.class);
   }
   
   public AlterOptions getAlterOptionsValue()
   {
     return (AlterOptions)getObjectValue(AlterOptions.class);
   }
   
   public ExtendOptions getExtendOptionsValue()
   {
     return (ExtendOptions)getObjectValue(ExtendOptions.class);
   }
   
   public VerifyOptions getVerifyOptionsValue()
   {
     return (VerifyOptions)getObjectValue(VerifyOptions.class);
   }
   
   public StatdumpOptions getStatdumpOptionsValue()
   {
     return (StatdumpOptions)getObjectValue(StatdumpOptions.class);
   }
   
   public CopyOptions getCopyOptionsValue()
   {
     return (CopyOptions)getObjectValue(CopyOptions.class);
   }
   
   public UnloadOptions getUnloadOptionsValue()
   {
     return (UnloadOptions)getObjectValue(UnloadOptions.class);
   }
   
   public AuditOptions getAuditOptionsValue()
   {
     return (AuditOptions)getObjectValue(AuditOptions.class);
   }
   
   public InfoOptions getInfoOptionsValue()
   {
     return (InfoOptions)getObjectValue(InfoOptions.class);
   }
   
   public VWInfoOptions getVWInfoOptionsValue()
   {
     return (VWInfoOptions)getObjectValue(VWInfoOptions.class);
   }
   
   public ConnectOptions getConnectOptionsValue()
   {
     return (ConnectOptions)getObjectValue(ConnectOptions.class);
   }
   
   public IIapi.Privileges getPrivilegeValue()
   {
     return (IIapi.Privileges)getObjectValue(IIapi.Privileges.class);
   }
   
   public SessionAuth.AuthType getAuthTypeValue()
   {
     return (SessionAuth.AuthType)getObjectValue(SessionAuth.AuthType.class);
   }
   
   public Object getValue()
   {
     return this.value;
   }
   
   public void setIntValue(int i)
   {
     this.value = Integer.valueOf(i);
   }
   
   public void setLongValue(long l)
   {
     this.value = Long.valueOf(l);
   }
   
   public void setBooleanValue(boolean b)
   {
     this.value = Boolean.valueOf(b);
   }
   
   public void setHandleValue(HandleBase handle)
   {
     this.value = handle;
   }
   
   public void setValue(Object obj)
   {
     this.value = obj;
   }
   
   public Parameter clone()
   {
     Parameter c = null;
     try
     {
       c = (Parameter)super.clone();
       
 
 
       c.value = null;
     }
     catch (CloneNotSupportedException cnse)
     {
       throw new RuntimeException(cnse);
     }
     return c;
   }
   
   public static Parameter[] cloneParameters(Parameter[] formalSet)
   {
     Parameter[] newParams = new Parameter[formalSet.length];
     for (int i = 0; i < formalSet.length; i++) {
       newParams[i] = formalSet[i].clone();
     }
     return newParams;
   }
   
   private <T> T[] readArray(Class<T> type, ObjectInputStream is, Object value, IO dir, IO inout)
     throws IOException, ClassNotFoundException
   {
     int len = is.readInt();
     
     T[] array = (T[])(len < 0 ? null : (dir != IO.INPUT) && (inout == IO.OUTPUT) && (this.ordinal != -1) ? Array.newInstance(type, len) : value != null ? value : Array.newInstance(type, len));
     if ((dir != IO.OUTPUT) || (inout != IO.INPUT)) {
       if (type.isArray()) {
         for (int i = 0; i < len; i++) {
           array[i] = type.cast(readArray(type.getComponentType(), is, array[i], dir, inout));
         }
       } else {
         for (int i = 0; i < len; i++) {
           array[i] = type.cast(is.readObject());
         }
       }
     }
     return array;
   }
   
   private byte[] readByteArray(ObjectInputStream is, Object value, IO dir, IO inout, int protoLvl)
     throws IOException, ClassNotFoundException
   {
     int len = is.readInt();
     
     byte[] array = (byte[])(len < 0 ? null : (dir != IO.INPUT) && (inout == IO.OUTPUT) && (this.ordinal != -1) ? new byte[len] : value != null ? value : new byte[len]);
     if ((dir != IO.OUTPUT) || (inout != IO.INPUT)) {
       if ((protoLvl >= 4) && (array != null)) {
         is.readFully(array);
       } else {
         for (int i = 0; i < len; i++) {
           array[i] = ((Byte)is.readObject()).byteValue();
         }
       }
     }
     return array;
   }
   
   
   public void read(ObjectInputStream is, IO inout, int protoLvl)
     throws ParameterException, InstantiationException, IllegalAccessException //@modify zhengxb add two exception 
   {
     String pName = getLongName();
     try
     {
       if ((this.dir == IO.INPUT) && (inout == IO.OUTPUT)) {
         return;
       }
       if ((this.dir == IO.OUTPUT) && (inout == IO.INPUT)) {
         if (!this.type.isArray())
         {
           try
           {
             this.value = this.type.newInstance();
           }
           catch (Exception ex)
           {
             throw new ParameterException(ex);
           }
           return;
         }
       }
       Logging.Debug("Starting Parameter.read(%1$s, dir=%2$s, inout=%3$s, protoLvl=%4$d)...", new Object[] { getShortName(), this.dir, inout, Integer.valueOf(protoLvl) });
        
       int callerOrdinal = is.readInt();
       int callerNameHash = is.readInt();
       if ((this.ordinal != callerOrdinal) || (this.name.hashCode() != callerNameHash))
       {
         String msg = Intl.formatString(RemoteCommand.pkg, "param.badNameOrdinal", new Object[] { Integer.valueOf(callerOrdinal), Integer.valueOf(callerNameHash), Integer.valueOf(this.ordinal), Integer.valueOf(this.name.hashCode()) });
         
         Logging.Error("Parameter %1$s: %2$s", new Object[] { pName, msg });
         throw new ParameterException(pName, msg);
       }
       if (this.type == Void.TYPE)
       {
         Logging.Debug("Parameter.read%1$s", new Object[] { pName });
         return;
       }
       try
       {
         if (this.type.isArray())
         {
           if (this.type.getComponentType() == Byte.TYPE) {
             this.value = readByteArray(is, this.value, this.dir, inout, protoLvl);
           } else {
             this.value = readArray(this.type.getComponentType(), is, this.value, this.dir, inout);
           }
         }
         else if ((this.dir != IO.INPUT) && (inout == IO.OUTPUT) && (this.ordinal != -1))
         {
           if (HandleBase.class.isAssignableFrom(this.type))
           {
             HandleBase handle = (HandleBase)is.readObject();
             ((HandleBase)this.value).update(handle);
           }
           else
           {
             this.value = this.type.cast(is.readObject());
           }
         }
         else {
           this.value = this.type.cast(is.readObject());
         }
         Logging.Debug("Parameter.read%1$s", new Object[] { toString() });
       }
       catch (ClassNotFoundException cnfe)
       {
         Logging.Except(pName, cnfe);
         throw new ParameterException(pName, cnfe.getMessage());
       }
       catch (InvalidClassException ice)
       {
         Logging.Except(pName, ice);
         throw new ParameterException(pName, ice.getMessage());
       }
       catch (ClassCastException cce)
       {
         Logging.Except(pName, cce);
         throw new ParameterException(pName, cce.getClass().getSimpleName());
       }
       catch (StreamCorruptedException sce)
       {
         Logging.Except(pName, sce);
         throw new ParameterException(pName, sce.getMessage());
       }
       catch (OptionalDataException ode)
       {
         Logging.Except(pName, ode);
         throw new ParameterException(pName, ode.getMessage());
       }
     }
     catch (IOException ioe)
     {
       Logging.Except(pName, ioe);
       throw new ParameterException(pName, ioe.getMessage());
     }
   }
   
   private void writeArray(ObjectOutputStream os, Object array, IO dir, IO inout, int protoLvl)
     throws IOException
   {
     int len = array == null ? -1 : Array.getLength(array);
     os.writeInt(len);
     if ((dir != IO.OUTPUT) || (inout != IO.INPUT)) {
       if (len > 0)
       {
         Class<?> componentType = array.getClass().getComponentType();
         if (componentType.isArray()) {
           for (int i = 0; i < len; i++) {
             writeArray(os, Array.get(array, i), dir, inout, protoLvl);
           }
         } else if ((componentType == Byte.TYPE) && (protoLvl >= 4)) {
           os.write((byte[])array);
         } else {
           for (int i = 0; i < len; i++) {
             os.writeObject(Array.get(array, i));
           }
         }
       }
     }
   }
   
   /**
    * 
    * @param os
    * @param inout
    * @param protoLvl
    * @throws ParameterException
    */
   public void write(ObjectOutputStream os, IO inout, int protoLvl)
     throws ParameterException
   {
     String nameValue = toString();
     try
     {
       if ((this.dir == IO.INPUT) && (inout == IO.OUTPUT)) {
         return;
       }
       if ((this.dir == IO.OUTPUT) && (inout == IO.INPUT) && (!this.type.isArray())) {
         return;
       }
       Logging.Debug("Starting Parameter.write(%1$s, dir=%2$s, inout=%3$s, protoLvl=%4$d)...", new Object[] { getShortName(), this.dir, inout, Integer.valueOf(protoLvl) });
       
       os.writeInt(this.ordinal);
       os.writeInt(this.name.hashCode());
       if (this.type == Void.TYPE) {
         return;
       }
       try
       {
         Logging.Debug("Parameter.write%1$s", new Object[] { nameValue });
         if (this.type.isArray()) {
           writeArray(os, this.value, this.dir, inout, protoLvl);
         } else {
           os.writeObject(this.value);
         }
       }
       catch (InvalidClassException ice)
       {
         Logging.Except(nameValue, ice);
         throw new ParameterException(nameValue, ice.getMessage());
       }
       catch (NotSerializableException nse)
       {
         Logging.Except(nameValue, nse);
         throw new ParameterException(nameValue, nse.getMessage());
       }
     }
     catch (IOException ioe)
     {
       Logging.Except(nameValue, ioe);
       throw new ParameterException(nameValue, ioe.getMessage());
     }
   }
 }
