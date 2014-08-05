package com.ingres.exception;

public class IngresException extends Exception {
	public IngresException() {
		super("IngresException");
	}

	public IngresException(String message) {
		super(message);
	}

	public IngresException(Throwable cause) {
		super(cause);
	}

	public IngresException(String message, Throwable cause) {
		super(message, cause);
	}
}