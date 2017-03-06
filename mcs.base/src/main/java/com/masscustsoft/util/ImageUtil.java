package com.masscustsoft.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.HttpClient;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.service.TempItem;
import com.masscustsoft.service.UriContext;
import com.masscustsoft.xml.BeanFactory;
import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleOp;

public class ImageUtil {
	
	public static final int IMAGE_CUT=1, IMAGE_STRETCH=2;
	
	public static InputStream doResizeImage(String resize, String rawExt, InputStream is) throws Exception{
		String ext=rawExt.toLowerCase();
		String[] ss=resize.split("\\*");
		int ww=LightUtil.decodeInt(ss[0]);
		int hh=LightUtil.decodeInt(ss[1]);
		String color=null;
		int corner=8;
		if (ss.length>2) color=ss[2];
		if (ss.length>3) corner=LightUtil.decodeInt(ss[3]);
		int option=0;
		if (ss.length>4){
			String tt=ss[4];
			if (tt.equals("stretch")) option|=IMAGE_STRETCH;
			if (tt.equals("cut")||tt.equals("true")) option|=IMAGE_CUT;
		}
		try{
			if (is==null || ext.equals("html") || ext.equals("htm") ||ext.equals("txt") || ext.equals("xml")) throw new Exception("File not found.");
			if (ext.equalsIgnoreCase("doc")||ext.equalsIgnoreCase("docx") || ext.equalsIgnoreCase("xls") || ext.equalsIgnoreCase("xlsx") || ext.equalsIgnoreCase("rtf") || ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")){
				if (LightStr.isEmpty((String)(GlbHelper.get("convertDoc")))) throw new Exception("No doc convert required");
				TempItem tmp=convertToPdf(Upload.getUpload().getContext().uri0,ext,LightUtil.getToken(null));
    			is=tmp.getInputStream();
    			ext="pdf";
    			ThreadHelper.postponeDelete(tmp);
    		}
			if (ext.equalsIgnoreCase("pdf")){
				if ("false".equals(GlbHelper.get("convertPdf"))) throw new Exception("No pdf convert required");
	    		TempItem tmp=getPdfShot(is);
    			is=tmp.getInputStream();
    			ThreadHelper.postponeDelete(tmp);
    		}
    		TempItem tmp = getResizedImage(is, ww, hh, color, corner>0?Math.min(ww, hh)/corner:0,option);
    		is.close();
    		is=tmp.getInputStream();
    		UriContext ctx = Upload.getUpload().getContext();
    		if (ctx!=null) ctx.name="resized.png";
		}
		catch (Exception ee){
			IRepository res=BeanFactory.getBeanFactory().getRepository("res");
			String fn="images/"+rawExt+".png";
			if (res.existResource(fn)==null) fn="images/unknown.png";
			is=res.getResource(fn);
			UriContext ctx = Upload.getUpload().getContext();
			if (ctx!=null) ctx.name=rawExt+".png";
		}
		return is;
	}

	public static TempItem getResizedImage(InputStream src, int imgWidth, int imgHeight, String color, int radius, int option)
			throws Exception {
		BufferedImage resizeImg=resizeImage(src,imgWidth,imgHeight,color,radius,option);
		
		TempItem temp = TempUtil.getTempFile();
		OutputStream os = temp.getOutputStream();
		saveImage(resizeImg,os);
		
		return temp;
	}

	public static void saveImage(BufferedImage img, OutputStream os) throws IOException{
		ImageIO.write(img, "png", os);
		os.flush();
		os.close();
	}
	
	public static synchronized BufferedImage resizeImage(InputStream src, int imgWidth, int imgHeight, String color, int radius, int option)
			throws Exception {
		BufferedImage img = ImageIO.read(src);
		src.close();
		
		if ((option&IMAGE_CUT)!=0){
			float r=1.0f*imgWidth/imgHeight;
			int ww=Math.round(img.getHeight()*r);
			int hh=Math.round(img.getWidth()/r);
			if (ww>img.getWidth()){
				ww=img.getWidth();
			}
			else
			if (hh>img.getHeight()){
				hh=img.getHeight();
			}
			img=img.getSubimage(0, 0, ww, hh);
		}
		
		int newW=imgWidth,newH=imgHeight; //stretch
		if ((option&IMAGE_STRETCH)==0){
			//no stretch, keep ratio
			if (imgHeight<=0) imgHeight=2180;
			float r=1.0f*img.getWidth()/img.getHeight();
			newW=Math.round(imgHeight*r);
			newH=Math.round(imgWidth/r);
			if (newW>imgWidth) newW=imgWidth; else newH=imgHeight;
		}
		
		ResampleOp  resampleOp = new ResampleOp (newW,newH);
		resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
		BufferedImage resizedImg = resampleOp.filter(img, null);
		
		if (radius>0 || !LightStr.isEmpty(color)){
			resizedImg=roundImageCorner(resizedImg, imgWidth, imgHeight, color, radius);
		}
		
		return resizedImg;
	}
	
	public static synchronized BufferedImage createCaptchaImage(int imgWidth, int imgHeight, int fontWeight, double spacing, Color color, String text)
			throws Exception {
		BufferedImage img=new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		
		FontRenderContext frc =new FontRenderContext(null, true, true);
		Font f=new Font("Serif", Font.BOLD+Font.ITALIC, fontWeight);
		Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
		attributes.put(TextAttribute.TRACKING, spacing);
		Font font = f.deriveFont(attributes);
		
		Rectangle2D r2D = font.getStringBounds(text, frc);
	    int rWidth = (int) Math.round(r2D.getWidth());
	    int rHeight = (int) Math.round(r2D.getHeight());
	    int rX = (int) Math.round(r2D.getX());
	    int rY = (int) Math.round(r2D.getY());

		g.setPaint(color);
        g.setFont(font);
        
        int x = (imgWidth - rWidth)/2-rX;
        int y = (imgHeight-rHeight)/2-rY;
        
        g.drawString(text, x, y);
        g.dispose();
        
		return img;
	}
	
	public static TempItem convertToPdf(String uri0, String ext, String token) throws Exception{
		String viewer=(String)GlbHelper.get("covertDoc"); if (viewer==null) viewer=""; viewer=viewer.trim();
		if (LightStr.isEmpty(viewer)) throw new Exception("No tool configured to convert");
		String url=Upload.getWebRoot()+uri0;
		String lnk=null;
		if (viewer.equalsIgnoreCase("verypdf")){
			String cvt=(String)GlbHelper.get("Viewer.verypdf.convert"); 
			if (cvt==null) cvt="ViewAsPDFPaper"; //doc2any
			String key=(String)GlbHelper.get("Viewer.verypdf.apikey"); if (key==null) key="";
			String convert="http://online.verypdf.com/api/?apikey="+key+"&app="+cvt+"&in_ext=."+ext+"&outfile=out.pdf&infile="+LightStr.encodeUrl(url+(url.contains("?")?"&":"?")+"t_t="+token);
			StringBuffer buf=new StringBuffer();
			HttpClient wc=new HttpClient();
			wc.doGet(convert, buf);
			lnk=LightStr.find(buf, "[Output] ", "<br>");
			if (LightStr.isEmpty(lnk)) throw new Exception(buf.toString());
		}
		TempItem temp = TempUtil.getTempFile();
		if (lnk!=null){
			HttpClient wc=new HttpClient();
			wc.setTimeout(120000);
			InputStream is=wc.doGetDownload(lnk);
			OutputStream os=temp.getOutputStream();
			StreamUtil.copyStream(is, os, 0);
			is.close();
			os.close();
		}
		return temp;
	}

	public static TempItem getPdfShot(InputStream src) throws Exception{
		TempItem temp = TempUtil.getTempFile();
		PDDocument doc = PDDocument.load(src);
		List<PDPage> pages = doc.getDocumentCatalog().getAllPages();
        if (pages.size()>0){
        	PDPage pg = pages.get(0);
        	BufferedImage img = pg.convertToImage();
        	OutputStream os = temp.getOutputStream();
    		ImageIO.write(img, "png", os);
    		os.close();
        }
		return temp;
	}
	
	public static TempItem RotateImage(InputStream src, String ext, boolean clockwise)
		throws Exception {
		BufferedImage img = ImageIO.read(src);
		int h = img.getHeight(), w = img.getWidth();
		
		BufferedImage rot = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);

	    double theta= Math.PI / 2;
	    if (!clockwise) theta=-theta;

	    AffineTransform xform = new AffineTransform();
	    xform.translate(0.5*h, 0.5*w);
	    xform.rotate(theta);
	    xform.translate(-0.5*w, -0.5*h);
	    
	    //AffineTransform xform = AffineTransform.getRotateInstance(theta,w/2, h/2);
	    Graphics2D g = (Graphics2D) rot.createGraphics();
	    g.drawImage(img, xform, null);
	    
	    g.dispose();
	    
	    TempItem temp = TempUtil.getTempFile();
		OutputStream os = temp.getOutputStream();
		ImageIO.write(rot, ext, os);
		os.close();
		return temp;
	}

	public static BufferedImage roundImageCorner(BufferedImage image, int imgW, int imgH, String color, int cornerRadius) {
	    int ww = image.getWidth();
	    int hh = image.getHeight();
	    
	    int w=ww,h=hh;
	    
	    Color c=Color.WHITE;
	    
	    if (!LightStr.isEmpty(color)){
	    	w=imgW;
	    	h=imgH;
	    	c=LightStr.decodeHtmlColor(color);
	    }
	    
	    BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2 = output.createGraphics();

	    // This is what we want, but it only does hard-clipping, i.e. aliasing
	    // g2.setClip(new RoundRectangle2D ...)

	    // so instead fake soft-clipping by first drawing the desired clip shape
	    // in fully opaque white with antialiasing enabled...
	    g2.setComposite(AlphaComposite.Src);
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	    g2.setColor(c);
	    g2.fill(new RoundRectangle2D.Float((w-ww)/2, (h-hh)/2, ww, hh, cornerRadius, cornerRadius));

	    // ... then compositing the image on top,
	    // using the white shape from above as alpha source
	    g2.setComposite(AlphaComposite.SrcAtop);
	    g2.drawImage(image, (w-ww)/2, (h-hh)/2, null);

	    g2.dispose();

	    return output;
	}
}
