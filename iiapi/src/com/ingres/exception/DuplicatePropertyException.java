package com.ingres.exception;

public class DuplicatePropertyException extends BasePropertyGridException {
	public DuplicatePropertyException(String propertyName) {
		super("A duplicate property was found: " + propertyName);
	}
}