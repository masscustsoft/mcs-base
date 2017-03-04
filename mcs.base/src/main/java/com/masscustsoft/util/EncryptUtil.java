package com.masscustsoft.util;

import org.acegisecurity.providers.encoding.ShaPasswordEncoder;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.UrlBase64;

public class EncryptUtil {
	public static String decrypt(String strInput, String strKeyValue) {
		/*
		 * This will use a supplied key, and encrypt the data This is the
		 * equivalent of DES/CBC/PKCS5Padding
		 */
		String input = strInput;
		String keyString = strKeyValue;
		byte[] key = keyString.getBytes();
		byte[] inputBStream = UrlBase64.decode(input.getBytes());
		byte[] outText = new byte[1024];
		try {
			StreamCipher dcryptcipher = new RC4Engine();
			dcryptcipher.init(false, new KeyParameter(key));
			dcryptcipher.processBytes(inputBStream, 0, inputBStream.length,
					outText, 0);
		} catch (Exception e) {
			LogUtil.error("decrypt Err:",e);
			LogUtil.dumpStackTrace(e);
		}
		return new String(outText).trim();
	}

	public static String encrypt(String strInput, String strKeyValue) {
		StreamCipher cipher = new RC4Engine();
		String inputString = strInput;
		String keyString = strKeyValue;
		byte[] key = keyString.getBytes();
		byte[] input = inputString.getBytes();
		byte[] cipherText = new byte[input.length];
		try {
			cipher.init(true, new KeyParameter(key));
			cipher.processBytes(input, 0, input.length, cipherText, 0);
		} catch (Exception e) {
			LogUtil.error("encrypt Err:",e);
			LogUtil.dumpStackTrace(e);
		}
		return new String(UrlBase64.encode(cipherText)).trim();
	}

	public static String saltPassword(String pass, String salt){
		if (pass.startsWith("\u001f")) return pass;
		ShaPasswordEncoder en = new ShaPasswordEncoder(256);
		return "\u001f"+en.encodePassword(pass, salt);
	}
	
	public static String encodePassword(String pass, String salt){
		ShaPasswordEncoder en = new ShaPasswordEncoder(256);
		return en.encodePassword(pass, salt);
	}
	
	public static final String __OBFUSCATE = "OBF:";

	public static String obfuscate(String s) {
		if (s.startsWith(__OBFUSCATE))
			return s;

		StringBuffer buf = new StringBuffer();
		byte[] b = s.getBytes();

		synchronized (buf) {
			buf.append(__OBFUSCATE);
			for (int i = 0; i < b.length; i++) {
				byte b1 = b[i];
				byte b2 = b[s.length() - (i + 1)];
				int i1 = 127 + b1 + b2;
				int i2 = 127 + b1 - b2;
				int i0 = i1 * 256 + i2;
				String x = Integer.toString(i0, 36);

				switch (x.length()) {
				case 1:
					buf.append('0');
				case 2:
					buf.append('0');
				case 3:
					buf.append('0');
				default:
					buf.append(x);
				}
			}
			return buf.toString();
		}
	}

	/* ------------------------------------------------------------ */
	public static String deobfuscate(String s) {
		if (s.startsWith(__OBFUSCATE))
			s = s.substring(4);
		else
			return s;

		byte[] b = new byte[s.length() / 2];
		int l = 0;
		for (int i = 0; i < s.length(); i += 4) {
			String x = s.substring(i, i + 4);
			int i0 = Integer.parseInt(x, 36);
			int i1 = (i0 / 256);
			int i2 = (i0 % 256);
			b[l++] = (byte) ((i1 + i2 - 254) / 2);
		}

		return new String(b, 0, l);
	}
}
