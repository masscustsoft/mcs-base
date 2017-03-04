package com.masscustsoft.api;

/**
 * If a object as a Script Environment implemented this interface. I can participate to the Macro.
 * 
 * @author JSong
 *
 */
public interface IValue {
	/**
	 * A variable interpreter.
	 * 
	 * @param key
	 */
	public String get(String key);
}
