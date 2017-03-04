package com.masscustsoft.util.inner;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileCacheItem {
	int size;
	File f;
	RandomAccessFile rf;
	
	public FileCacheItem(byte[] buf) throws Exception{
		f=File.createTempFile("wab", "tmp");
		rf = new RandomAccessFile(f, "rw");
		FileChannel fc=rf.getChannel();
		fc.position(0);
		fc.write(ByteBuffer.wrap(buf));
		size=buf.length;
		//if (size<4096) fc.force(true);
	}
	
	public byte[] getBinary() throws IOException{
		FileChannel fc=rf.getChannel();
		ByteBuffer buf=ByteBuffer.allocate(size);
		fc.position(0);
		fc.read(buf);
		return buf.array();
	}
	
	public void destroy() throws Exception{
		rf.close();
		f.delete();
	}
}
