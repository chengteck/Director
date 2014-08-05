package com.ingres.exception;

public class NotInitializedException extends IngresException {
	public NotInitializedException() {
		super("Actian API layer not initialized");
	}
}