package com.masscustsoft.api;

import com.masscustsoft.Lang.DynamicClassInfo;

/**
 * A interface for dynamically generated classed based on ASM.
 * 
 * 
 * @author JSong
 *
 */
public interface IVariant {

	/**
	 * Get Java byte code by given INFO parameter
	 * 
	 * @param info Used as different type indicator to generate the class from data binded. 
	 * @throws Exception
	 */
	public byte[] getClassBytes(DynamicClassInfo info) throws Exception;
	
}
