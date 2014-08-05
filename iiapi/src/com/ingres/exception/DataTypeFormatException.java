package com.ingres.exception;

public class DataTypeFormatException extends IllegalArgumentException {
	public DataTypeFormatException() {
	}

	public DataTypeFormatException(String message) {
		super(String.format("%1$s: %2$s", new Object[] {
				DataTypeFormatException.class.getSimpleName(), message }));
	}

	public DataTypeFormatException(String message, Throwable cause) {
		super(String.format("%1$s: %2$s", new Object[] {
				DataTypeFormatException.class.getSimpleName(), message }),
				cause);
	}

	public DataTypeFormatException(Throwable cause) {
		super(cause);
	}
}