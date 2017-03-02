package com.masscustsoft.api;

/**
 * --en Interface for global clean up
 * --zh_cn 全局清理接口
 *  
 * @author JSong
 *
 */

public interface ICleanup {
	/**
	 * --en Global Object in GlbHelper which implemented this interface, this method is called to clean up while exit.
	 * --zh_cn GlbHelper的对象，如果实现此接口，当系统退出时，该方法被调用以完成清洁。
	 */
	public void cleanup();
}
