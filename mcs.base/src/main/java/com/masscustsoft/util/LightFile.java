package com.masscustsoft.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class LightFile {

	public static Color decodeHtmlColor(String colorString) {
		Color color;

		if (colorString.startsWith("#")) {
			colorString = colorString.substring(1);
		}
		if (colorString.endsWith(";")) {
			colorString = colorString.substring(0, colorString.length() - 1);
		}

		int red, green, blue;
		switch (colorString.length()) {
		case 6:
			red = Integer.parseInt(colorString.substring(0, 2), 16);
			green = Integer.parseInt(colorString.substring(2, 4), 16);
			blue = Integer.parseInt(colorString.substring(4, 6), 16);
			color = new Color(red, green, blue);
			break;
		case 3:
			red = Integer.parseInt(colorString.substring(0, 1), 16);
			green = Integer.parseInt(colorString.substring(1, 2), 16);
			blue = Integer.parseInt(colorString.substring(2, 3), 16);
			color = new Color(red, green, blue);
			break;
		case 1:
			red = green = blue = Integer.parseInt(colorString.substring(0, 1), 16);
			color = new Color(red, green, blue);
			break;
		default:
			throw new IllegalArgumentException("Invalid color: " + colorString);
		}
		return color;
	}
	
	public static long copyStream(InputStream is, OutputStream os, int max) throws IOException {
		long total=0;
		byte[] buf = new byte[8192];
		int rawmax = max;
		int size = 0;
		do {
			size = is.read(buf);
			if (size > 0) {
				if (rawmax == 0) {
					os.write(buf, 0, size);
					total+=size;
				} else {
					if (size < max) {
						os.write(buf, 0, size);
						max -= size;
					} else {
						os.write(buf, 0, max);
						max = 0;
					}
					total+=size;
					if (max == 0) {
						break;
					}
				}
			}
		} while (size != -1);
		return total;
	}
	
	public static void copyFile(File src, File dst) throws Exception {
		FileInputStream is = new FileInputStream(src);
		FileOutputStream os = new FileOutputStream(dst);
		copyStream(is, os, 0);
		os.close();
		is.close();
	}

	public static void loadStream(InputStream f, StringBuffer buf, String enc) throws IOException {
		if (enc == null)
			enc = "8859_1";
		BufferedReader r = new BufferedReader(new InputStreamReader(f, enc));
		while (true) {
			String st = r.readLine();
			if (st == null) break;
			//if (st.length() == 0) continue;
			//char ch = 65279;
			//if (st.startsWith(ch + ""))
			//st = st.substring(1);
			buf.append(st + "\r\n");
		}
		//r.close();
	}

	public static void saveStream(OutputStream f, StringBuffer buf, String enc) {
		if (enc == null)
			enc = "8859_1";
		try {
			Writer w = new OutputStreamWriter(f, enc);
			w.append(buf.toString());
			w.flush();
			w.close();
		} catch (IOException e) {
			LogUtil.info("saveStream fail:"+e);
		}
	}	
}
