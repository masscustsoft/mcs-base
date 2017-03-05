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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.masscustsoft.helper.Upload;
import com.masscustsoft.util.inner.StreamCrc;

public class StreamUtil {

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
	
	public static long getFileCRC(File f) throws IOException {
		CRC32 digest = new CRC32();
		FileInputStream is = new FileInputStream(f);
		byte[] buf = new byte[1024];
		while (true) {
			int size = is.read(buf);
			if (size < 0)
				break;
			digest.update(buf, 0, size);
		}
		is.close();
		return digest.getValue();
	}

	private static Map<String, String> getJarInfo(File f1) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		JarFile jar = new JarFile(f1);
		for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
			JarEntry je = e.nextElement();
			map.put(je.getName(), je.getSize() + ":" + je.getCompressedSize()
					+ ":" + je.getCrc());
		}
		jar.close();
		return map;
	}

	public static boolean sameJar(File f1, File f2) throws Exception {
		System.out.println("sameJar "+f1.getAbsolutePath()+",f2="+f2.getAbsolutePath());
		Map<String, String> aa = getJarInfo(f1);
		Map<String, String> bb = getJarInfo(f2);
		Set<String> keys = aa.keySet();
		for (String key : keys) {
			String v1 = aa.get(key);
			String v2 = bb.get(key);
			if (v2 == null)
				return false;
			if (!v2.equals(v1))
				return false;
			bb.remove(key);
		}
		if (bb.size() > 0)
			return false;
		return true;
	}
	
	public static boolean sameFile(File g1, File g2) throws Exception {
		if (g1.getName().endsWith(".jar"))
			return sameJar(g1, g2);
		if (g1.length() != g2.length())
			return false;
		long c1 = getFileCRC(g1);
		long c2 = getFileCRC(g2);
		//LogUtil.info(g1.getAbsolutePath() + ".crc=" + c1+", "+g2.getAbsolutePath()+".crc="+c2);
		if (c1 != c2)
			return false;
		return true;
	}
	
	private static long[] crc32Table=null;
	
	public static long crc32(String str) {
	    if (crc32Table==null){
	    	crc32Table = new long[256];
	        for (int i = 0; i < 256; i++) {
	            long k = i;
	            for (int j = 0; j < 8; j++)
	                if ((k&1)!=0)
	                    k = (k >> 1) ^ 0xedb88320;
	                else k >>= 1;
	            crc32Table[i] = k;
	        }
	    }
	    long crc = 0xffffffff;
	    for (int i = 0; i < str.length(); i++) {
	        long code = str.charAt(i);
	        if (code > 0xff) {
	            crc = (crc >> 8) ^ crc32Table[(int)((crc&0xff)^(code&0xff))];
	            crc = (crc >> 8) ^ crc32Table[(int)((crc & 0xff) ^ (code >> 8))];
	        } else crc = (crc >> 8) ^ crc32Table[(int)((crc & 0xff) ^ code)];
	    }
	    return crc ^ 0xffffffff;
	};

	public static StreamCrc crc32(byte[] str) {
		StreamCrc crc=new StreamCrc();
		crc.crc=crc32(str,0,str.length, null);
		if (crc.crc<0) crc.crc=-crc.crc;
		crc.size=str.length;
		return crc;
	}
	
	public static StreamCrc crc32(InputStream is) throws Exception{
		StreamCrc m=new StreamCrc();
		int Z=256;
		byte[] str=new byte[Z];
		int sz=Z;
		long crc=0,tot=0;
		while (sz==Z){
			crc ^= 0xffffffff;
			sz=is.read(str);
			crc=crc32(str,0,str.length,crc);
			tot+=sz;
		}
		if (crc<0) crc=-crc;
		m.size=tot;
		m.crc=crc;
		is.close();
		return m;
	}
	
	private static long crc32(byte[] str,int from, int to, Long crc0) {
	    if (crc32Table==null){
	    	crc32Table = new long[256];
	        for (int i = 0; i < 256; i++) {
	            long k = i;
	            for (int j = 0; j < 8; j++)
	                if ((k&1)!=0)
	                    k = (k >> 1) ^ 0xedb88320;
	                else k >>= 1;
	            crc32Table[i] = k;
	        }
	    }
	    long crc = crc0==null?0xffffffff:crc0;
	    for (int i = from; i < str.length && i<to; i++) {
	        long code = str[i]<0?256+str[i]:str[i]; 
	        if (code > 0xff) {
	            crc = (crc >> 8) ^ crc32Table[(int)((crc&0xff)^(code&0xff))];
	            crc = (crc >> 8) ^ crc32Table[(int)((crc & 0xff) ^ (code >> 8))];
	        } else crc = (crc >> 8) ^ crc32Table[(int)((crc & 0xff) ^ code)];
	    }
	    return crc ^ 0xffffffff;
	}
	
	public static void streamOut(Upload up,InputStream is, Long size) throws Exception{
		streamOut(up, is, null, size, null);
	}
	
	public static void streamOut(Upload up,InputStream is, String fileName, Long size, Long modi) throws Exception{
		HttpServletResponse resp=up.getResponse();
		HttpServletRequest req=up.getRequest();
		streamOut(req,resp,is,fileName,size,modi);
	}
	
	private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
            || Arrays.binarySearch(matchValues, "*") > -1;
    }

	private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

	public static void streamOut(HttpServletRequest req,HttpServletResponse resp,InputStream is, String fileName, Long size, Long modi) throws Exception{
		long length = size==null?0:size;
        long lastModified = modi==null?System.currentTimeMillis():modi;
        String eTag = fileName==null?null:fileName + "_" + length + "_" + lastModified;
        long expires = System.currentTimeMillis() + 8*3600*1000;

        resp.setBufferSize(10240);
        	
        //resp.setHeader("Cache-Control", "private");
		boolean content=!"head".equals(ThreadHelper.get("reqType"));
		
        if (length==0){
        	LightUtil.doCache(resp);
        	//if no size set, response as before
        	if (content) {
        		ServletOutputStream out = resp.getOutputStream();
            	copyStream(is, out, 0);
            	is.close();
            }
        	return;
        }
        resp.setHeader("Cache-Control","maxage=3600");
      	resp.setHeader("Pragma","public");
              
        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = req.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            resp.setHeader("ETag", eTag); // Required in 304.
            resp.setDateHeader("Expires", expires); // Postpone cache with 1 week.
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = req.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            resp.setHeader("ETag", eTag); // Required in 304.
            resp.setDateHeader("Expires", expires); // Postpone cache with 1 week.
            return;
        }


        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = req.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = req.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }


        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<Range>();

        // Validate and process Range and If-Range headers.
        String range = req.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                resp.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = req.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = req.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        resp.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }

		resp.setHeader("Accept-Ranges", "bytes");
		if (eTag!=null){
			resp.setHeader("ETag", eTag);
			resp.setDateHeader("Last-Modified", lastModified);
			resp.setDateHeader("Expires", expires);
		}
		
		if (size!=null) resp.setContentLength(size.intValue());
		ServletOutputStream out = resp.getOutputStream();
		
		try{
			if (ranges.isEmpty() || ranges.get(0) == full) {

				// Return full file.
                Range r = full;
                resp.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);

                if (content) {
                	resp.setHeader("Content-Length", String.valueOf(r.length));

                    // Copy full range.
                    is.skip(r.start);
                    copyStream(is, out, (int)r.length);
                }

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                resp.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                resp.setHeader("Content-Length", String.valueOf(r.length));
                resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                	// Copy single part range.
                	is.skip(r.start);
                    copyStream(is, out, (int)r.length);
                }

            } else {

            	String MULTIPART_BOUNDARY = "MULTIPART_"+LightUtil.getHashCode();
                // Return multiple parts of file.
                resp.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                	// Cast back to ServletOutputStream to get the easy println methods.
                    ServletOutputStream sos = (ServletOutputStream) out;

                    // Copy multi part range.
                    for (Range r : ranges) {
                    	// Add multipart boundary and header fields for every range.
                        sos.println();
                        sos.println("--" + MULTIPART_BOUNDARY);
                        sos.println("Content-Type: " + resp.getContentType());
                        sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                        // Copy single part range of multi part range.
                        is.skip(r.start);
                        copyStream(is, out, (int)r.length);
                    }

                    // End with multipart boundary.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY + "--");
                }
            }
		}
		finally{
			if (is!=null) is.close();
		}
	}
}


class Range {
    long start;
    long end;
    long length;
    long total;

    /**
     * Construct a byte range.
     * @param start Start of the byte range.
     * @param end End of the byte range.
     * @param total Total length of the byte source.
     */
    public Range(long start, long end, long total) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
        this.total = total;
    }

}