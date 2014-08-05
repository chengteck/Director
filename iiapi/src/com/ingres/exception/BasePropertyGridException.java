package com.ingres.exception;

public abstract class BasePropertyGridException extends IngresException {
	public BasePropertyGridException() {
	}

	public BasePropertyGridException(String message) {
		super(message);
	}
}