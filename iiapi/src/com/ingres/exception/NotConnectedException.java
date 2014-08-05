package com.ingres.exception;

public class NotConnectedException extends IngresException {
	public NotConnectedException() {
		super("Not connected to Ingres server.");
	}
}