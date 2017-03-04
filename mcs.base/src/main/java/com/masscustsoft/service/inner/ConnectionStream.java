package com.masscustsoft.service.inner;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class ConnectionStream extends InputStream {
	HttpURLConnection conn;
	InputStream is;
	
	public ConnectionStream(HttpURLConnection conn,InputStream is){
		this.conn=conn;
		this.is=is;
	}
	
	@Override
	public int read() throws IOException {
		return is.read();
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
		conn.disconnect();
	}

	@Override
	public synchronized void mark(int readlimit) {
		is.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return is.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return is.read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		is.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return is.skip(n);
	}

}
