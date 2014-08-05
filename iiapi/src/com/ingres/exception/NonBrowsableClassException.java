package com.ingres.exception;

public class NonBrowsableClassException extends BasePropertyGridException {
	public NonBrowsableClassException() {
		super("An attempt was made to process a Non-Browsable class.");
	}
}
