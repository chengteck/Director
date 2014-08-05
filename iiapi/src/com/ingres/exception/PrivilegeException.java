package com.ingres.exception;

import com.ingres.IIapi;
import com.ingres.IIapi.Exception;

public class PrivilegeException extends IIapi.Exception {
	public PrivilegeException() {
		super("PrivilegeException");
	}

	public PrivilegeException(String msg) {
		super(String.format("PrivilegeException: %1$s", new Object[] { msg }));
	}
}