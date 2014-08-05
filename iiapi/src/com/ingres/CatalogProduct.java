package com.ingres;

public enum CatalogProduct
{
  INGRES("ingres"), 

  INGRES_DBD("ingres/dbd"), 

  VISION("vision"), 

  WINDOWS_4GL("windows_4gl"), 

  NONE("nofeclients");

  private String flagString;

  private CatalogProduct(String flagString) {
    this.flagString = flagString;
  }

  public String toString()
  {
    return this.flagString;
  }
}