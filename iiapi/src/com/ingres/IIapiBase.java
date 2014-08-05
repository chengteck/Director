package com.ingres;

import java.io.File;

public abstract class IIapiBase
  implements IIapi
{
  public ConnectionHandle connectNameServer(String target, String user, byte[] password, int iTimeout)
  {
    return connectNameServer(target, user, password, iTimeout, null);
  }

  public ConnectionHandle connectDatabase(String target, String user, byte[] password, int iTimeout)
  {
    return connectDatabase(target, user, password, iTimeout, null);
  }

  public StatementHandle executeProcedure(ConnectionHandle connHandle, TransactionHandle tranHandle, String procName, String procOwner, IIapi.Descriptor[] descriptors, IIapi.DataValue[] datavalues)
  {
    if (procName == null) {
      throw new IllegalArgumentException("DB procedure name required.");
    }
    int additionalParamCount = 1;
    if (procOwner != null) {
      additionalParamCount = 2;
    }
    int descCount = descriptors == null ? 0 : descriptors.length;
    int dataCount = datavalues == null ? 0 : datavalues.length;
    int paramCount = descCount;

    if (descCount != dataCount) {
      throw new IllegalArgumentException("Descriptor array and DataValue array are not the same length.");
    }
    IIapi.Descriptor[] fullDesc = new IIapi.Descriptor[descCount + additionalParamCount];
    IIapi.DataValue[] fullData = new IIapi.DataValue[dataCount + additionalParamCount];
    int i = 0;
    
    //@modify zxbing int to short tranform
    fullDesc[i] = new IIapi.Descriptor((short)20, false, procName.length(), (short)3, null);
    fullData[i] = new IIapi.DataValue(procName);
    i++;

    if (procOwner != null) {
      
      //@modify zxbing int to short tranform
      fullDesc[i] = new IIapi.Descriptor((short)20, false, procOwner.length(), (short)3, null);
      fullData[i] = new IIapi.DataValue(procOwner);
      i++;
    }

    if (paramCount > 0) {
      System.arraycopy(descriptors, 0, fullDesc, i, paramCount);
      System.arraycopy(datavalues, 0, fullData, i, paramCount);
    }

    return executeProcedure(connHandle, tranHandle, fullDesc, fullData);
  }

  public void PMload(ContextHandle ctxHandle, File f)
  {
    PMload(ctxHandle, f.getAbsolutePath());
  }

  public void PMwrite(ContextHandle ctxHandle, File f)
  {
    PMwrite(ctxHandle, f.getAbsolutePath());
  }

  public String getLogFilesDirectory()
  {
    String logLoc = getEnv("II_LOG");

    if ((logLoc == null) || (logLoc.isEmpty())) {
      logLoc = getEnv("II_CONFIG");
    }

    if ((logLoc == null) || (logLoc.isEmpty()))
    {
      logLoc = String.format("%1$s/%2$s", new Object[] { getSysInfo().IIsystem, "ingres/files" });
    }

    return logLoc;
  }
}