package com.masscustsoft.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocListener;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactoryImp;
import com.itextpdf.text.FontProvider;
import com.itextpdf.text.GreekList;
import com.itextpdf.text.Image;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.RomanList;
import com.itextpdf.text.TextElementArray;
import com.itextpdf.text.ZapfDingbatsList;
import com.itextpdf.text.ZapfDingbatsNumberList;
import com.itextpdf.text.html.HtmlTags;
import com.itextpdf.text.html.HtmlUtilities;
import com.itextpdf.text.html.simpleparser.CellWrapper;
import com.itextpdf.text.html.simpleparser.ChainedProperties;
import com.itextpdf.text.html.simpleparser.ElementFactory;
import com.itextpdf.text.html.simpleparser.HTMLTagProcessor;
import com.itextpdf.text.html.simpleparser.HTMLTagProcessors;
import com.itextpdf.text.html.simpleparser.HTMLWorker;
import com.itextpdf.text.html.simpleparser.ImageProcessor;
import com.itextpdf.text.html.simpleparser.ImageProvider;
import com.itextpdf.text.html.simpleparser.ImageStore;
import com.itextpdf.text.html.simpleparser.LinkProcessor;
import com.itextpdf.text.html.simpleparser.StyleSheet;
import com.itextpdf.text.html.simpleparser.TableWrapper;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import com.itextpdf.text.pdf.Barcode;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.Barcode39;
import com.itextpdf.text.pdf.BarcodeCodabar;
import com.itextpdf.text.pdf.BarcodeEAN;
import com.itextpdf.text.pdf.BarcodeEANSUPP;
import com.itextpdf.text.pdf.BarcodeInter25;
import com.itextpdf.text.pdf.BarcodePDF417;
import com.itextpdf.text.pdf.BarcodePostnet;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEvent;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.Base64;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.itextpdf.text.xml.simpleparser.SimpleXMLParser;
import com.masscustsoft.api.IRepository;
import com.masscustsoft.helper.Upload;
import com.masscustsoft.model.AbstractResult;
import com.masscustsoft.model.ExternalFile;
import com.masscustsoft.model.ReportFile;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.MapUtil;
import com.masscustsoft.util.StreamUtil;
import com.masscustsoft.util.ThreadHelper;
import com.masscustsoft.xml.BeanFactory;

public class ToPdf extends DirectAction implements PdfPageEvent{
	
	protected Map getRpt() throws Exception{
		Map rpt = (Map)LightUtil.parseJson(getStr("rpt","{}"));
		return rpt;
	}
	
	@Override
	protected void run(AbstractResult ret) throws Exception {
		Map<String,String> i18n= (Map)LightUtil.parseJson(getStr("i18n","{}"));
		ThreadHelper.set("_i18n_", i18n);
		
		Map rpt = getRpt();
		
		File temp=File.createTempFile("tmp", ".pdf");
		
		String name=MapUtil.getStr(rpt,"title","No-Name");
		HttpServletResponse resp = Upload.getUpload().getResponse();
		resp.setHeader("Content-Disposition", "attachment;filename=\"report.pdf\"");
		resp.setContentType("application/pdf");
		LightUtil.doCache(resp);
		createPdf(rpt, i18n, temp);
		
		FileInputStream is = new FileInputStream(temp);
		PdfReader reader=new PdfReader(is);
		int pages=reader.getNumberOfPages();
		File temp2=File.createTempFile("pdf", ".pdf");
		
		OutputStream out=new FileOutputStream(temp2);
		PdfStamper stamp=new PdfStamper(reader,out);
		for (int i=1;i<=pages;i++){
			PdfContentByte c=stamp.getOverContent(i);
			Rectangle ps = reader.getPageSize(i);
			ThreadHelper.set("pageNumber", i);
			ThreadHelper.set("pageCount", pages);
			c.saveState();
			
	    	List<Map> items=(List)rpt.get("overlays");
			if (items!=null){
				for (Map item:items){
					getDirectContent(c,ps,item);		
				}
			}

	        c.restoreState();
		}
		stamp.close();
		
		ReportFile df=new ReportFile();
		df.setOwnerId(getSession().getUserId());
		df.setName(name);
		df.setCreateTime(LightUtil.longDate());
		ExternalFile.newExternalFile(getDs(), getFs(), df.getFile(), temp2);
		getDs().insertBean(df);
		
		temp.delete();
		temp2.delete();
		
		Map m=new HashMap();
		m.put("externalId", df.getFile().getExternalId());
		ret.setResult(m);
	}
	
	private void createPdf(Map rpt, Map<String,String> i18n, File pdf) throws Exception{
		OutputStream out=new FileOutputStream(pdf);
		
		ThreadHelper.set("_fonts_",new HashMap<String,BaseFont>());
		ThreadHelper.set("_defaultFont_",BaseFont.createFont());
		
		String pageSize=MapUtil.getStr(rpt,"pageSize","A4");
		int defaultFontSize=MapUtil.getInt(rpt,"defaultFontSize",6);
		ThreadHelper.set("_defaultFontSize_",defaultFontSize);
		ThreadHelper.set("_rpt_",rpt);
		
		int i=pageSize.indexOf(';');
		String margins="36 36 36 36";
		if (i>0){
			margins=pageSize.substring(i+1);
			pageSize=pageSize.substring(0,i);
		}
		boolean rotate=false;
		if (pageSize.startsWith("@")) { rotate=true; pageSize=pageSize.substring(1);}
		Rectangle pSize = PageSize.getRectangle(pageSize);
		if (rotate) pSize=pSize.rotate();
		
		String mars[]=margins.split(" ");
		float ml=0,mt=0,mr=0,mb=0;
		mr=mt=mb=ml=LightUtil.decodeFloat(mars[0]);
		if (mars.length>1) {
			mt=mb=LightUtil.decodeFloat(mars[1]);
		}
		if (mars.length>2){
			mr=LightUtil.decodeFloat(mars[2]);
		}
		if (mars.length>3){
			mb=LightUtil.decodeFloat(mars[3]);
		}
		Document doc=new Document(pSize,ml,mr,mt,mb);
		MapUtil.setIfStr(rpt, "author", doc, "addAuthor");
		MapUtil.setIfStr(rpt, "creator", doc, "addCreator");
		MapUtil.setIfStr(rpt, "title", doc, "addTitle");
		MapUtil.setIfStr(rpt, "keyWords", doc, "addKeywords");
		MapUtil.setIfStr(rpt, "subject", doc, "addSubject");
		
		PdfWriter writer = PdfWriter.getInstance(doc, out);
		writer.setPageEvent(this);
		writer.setStrictImageSequence(true);
		ThreadHelper.set("_writer_", writer);
		ThreadHelper.set("_doc_", doc);
		doc.open();
		
		List<Map> items=(List)rpt.get("items");
		for (Map row:items){
			Element el=getElement(row);
			if (el!=null) doc.add(el);
		}
	    doc.close();
		out.close();
		writer.close();
		
		ThreadHelper.set("_writer_", null);
		ThreadHelper.set("_doc_", null);
		
	}

	private Map<String,BaseFont> getFonts(){
		return (Map)ThreadHelper.get("_fonts_");
	}
	
	private BaseFont getDefaultFont(){
		return (BaseFont)ThreadHelper.get("_defaultFont_");
	}
	
	@Override
	public void onOpenDocument(PdfWriter writer, Document document) {
		PdfTemplate pgTpl = writer.getDirectContent().createTemplate(100, 100);
		pgTpl.setBoundingBox(new Rectangle(-20, -20, 100, 100));
        ThreadHelper.set("_pageTpl_", pgTpl);
	}

	private PdfTemplate getPgTpl(){
		return (PdfTemplate)ThreadHelper.get("_pageTpl_");
	}
	
	@Override
	public void onCloseDocument(PdfWriter writer, Document document) {
		PdfTemplate tpl = getPgTpl();
		tpl.beginText();
	    tpl.setFontAndSize(getDefaultFont(), 12);
	    tpl.setTextMatrix(0, 0);
	    tpl.showText(Integer.toString(writer.getPageNumber() - 1));
	    tpl.endText();
	}

	@Override
	public void onStartPage(PdfWriter writer, Document document) {
		
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
	}

	@Override
	public void onParagraph(PdfWriter writer, Document document, float paragraphPosition) {
		
	}

	@Override
	public void onParagraphEnd(PdfWriter writer, Document document, float paragraphPosition) {
		
	}

	@Override
	public void onChapter(PdfWriter writer, Document document, float paragraphPosition, Paragraph title) {
		
	}

	@Override
	public void onChapterEnd(PdfWriter writer, Document document, float paragraphPosition) {
		
	}

	@Override
	public void onSection(PdfWriter writer, Document document, float paragraphPosition, int depth, Paragraph title) {
		
	}

	@Override
	public void onSectionEnd(PdfWriter writer, Document document, float paragraphPosition) {
		
	}

	@Override
	public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
		
	}
	
	private PdfWriter getWriter(){
		return (PdfWriter)ThreadHelper.get("_writer_");
	}
	
	private Document getDoc(){
		return (Document)ThreadHelper.get("_doc_");
	}
	
	private Element getElement(Map it) throws DocumentException, Exception {
		String cls=MapUtil.getStr(it,"cls");
		switch(cls){
		case "separator":
			return getSeparator(it);
		case "image":
			return getImage(it);
		case "list":
			return getList(it);
		case "table":
			return getTable(it);
		case "barcode":
			return getBarcode(it);
		case "newpage":
			getDoc().newPage();
		default:
			return getParagraph(it);
		}
	}
	
	private LineSeparator getSeparator(Map it) throws Exception{
		LineSeparator sep = new LineSeparator();
		MapUtil.setIfFloat(it, "lineWidth", sep);
		MapUtil.setIfFloat(it, "offset", sep);
		MapUtil.setIfFloat(it, "percentage", sep);
		BaseColor color=getColor(it,"color");
		if (color!=null) sep.setLineColor(color);
		sep.setAlignment(getAlignment(it,"alignment"));
		return sep;
	}
	
	private Paragraph getParagraph(Map it) throws Exception{
		Paragraph p=new Paragraph();
		String lb=MapUtil.getStr(it,"label", null);
		if (lb!=null){
			Chunk ch=new Chunk();
			ch.setLocalDestination(lb);
			p.add(ch);
		}
		getChunks(p,it,null);
		List<Map> lst=(List)it.get("items");
		if (lst!=null){
			for (Map item:lst){
				Element el=getElement(item);
				if (el!=null) p.add(el);
			}
		}
		applyFont(p,it);
		
		return p;	
	}
	
	private void applyFont(Paragraph p, Map it) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		if (it.get("pushFontSize")!=null){
			Stack<Integer> stack=(Stack)ThreadHelper.get("_fontSizeStack_");
			if (stack==null){
				stack=new Stack();
				ThreadHelper.set("_fontSizeStack_",stack);
			}
			int cur=(Integer)ThreadHelper.get("_defaultFontSize_");
			int size=MapUtil.getInt(it, "pushFontSize", 6);
			stack.push(cur);
			ThreadHelper.set("_defaultFontSize_",size);	
		}
		if (it.get("popFontSize")!=null){
			Stack<Integer> stack=(Stack)ThreadHelper.get("_fontSizeStack_");
			if (stack!=null && stack.size()>0){
				ThreadHelper.set("_defaultFontSize_",stack.pop());	
			}
		}
		MapUtil.setIfFloat(it, "leading", p);
		MapUtil.setIfFloat(it, "firstLineIndent", p);
		MapUtil.setIfFloat(it, "indentationLeft", p);
		MapUtil.setIfFloat(it, "indentationRight", p);
		MapUtil.setIfFloat(it, "spacingAfter", p);
		MapUtil.setIfFloat(it, "spacingBefore", p);
		p.setAlignment(getAlignment(it,"alignment"));
		for (Object a:p.getChunks()){
			if (a instanceof Chunk){
				Chunk ch=(Chunk)a;
				applyFont(ch,it);
			}
		}
	}
	
	private int getAlignment(Map it,String fld){
		String alignment=MapUtil.getStr(it, fld);
		int align=Element.ALIGN_UNDEFINED;
		if ("right".equalsIgnoreCase(alignment)) align=Element.ALIGN_RIGHT;
		if ("left".equalsIgnoreCase(alignment)) align=Element.ALIGN_LEFT;
		if ("center".equalsIgnoreCase(alignment)) align=Element.ALIGN_CENTER;
		return align;
	}
	
	private int getVAlign(Map it,String fld){
		String alignment=MapUtil.getStr(it, fld);
		int align=Element.ALIGN_UNDEFINED;
		if ("bottom".equalsIgnoreCase(alignment)) align=Element.ALIGN_BOTTOM;
		if ("top".equalsIgnoreCase(alignment)) align=Element.ALIGN_TOP;
		if ("middle".equalsIgnoreCase(alignment)) align=Element.ALIGN_MIDDLE;
		return align;
	}
	
	private void getChunks(Paragraph p, final Map it, String text) throws Exception{
		if (text==null){
			text=(String)it.get("text");
		}
		if (text!=null){
			if (text.startsWith("<") || text.endsWith(">")){
				//treat as HTML
				Reader in=new StringReader(text);
				FontFactoryImp ffi = new FontFactoryImp(){
					@Override
					public Font getFont(String fontname, String encoding, boolean embedded, float size, int style, BaseColor color, boolean cached) {
						if (size==Font.UNDEFINED) size=getSize(it);
						 if (style==Font.UNDEFINED) style=getStyle(it);
						 if (color==null) color=getColor(it,"color");
						 return super.getFont(fontname, encoding, embedded, size, style, color, cached);
					}
				};
				HashMap map=new HashMap();
				map.put("font_factory",ffi);
				List list= DirectHtmlWorker.parse2List(in, null, null, map);
				for (int i=0;i<list.size();i++){
					Object el=list.get(i);
					if (el instanceof Paragraph){
						Paragraph pp=(Paragraph)el;
						applyFont(pp,it);
						p.add(pp);
					}
				}
				return;
			}
			List<BaseFont> base=new ArrayList<BaseFont>();
			String st=text;
			StringBuffer buf=new StringBuffer();
			for (int i=0;i<st.length();i++){
				char c=st.charAt(i);
				if (!compatible(base,c)){
					processMacro(buf.toString(), base.get(0), it, p);
					buf.delete(0,buf.length());
					base.clear();
				}
				buf.append(c);
			}
			if (buf.length()>0){
				processMacro(buf.toString(), base.get(0), it, p);
			}
		}
	}
	
	private void processMacro(String buf, BaseFont base, Map it, Paragraph p) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		List<String> list=LightUtil.splitMacro(buf, '$');
		for (String ss:list){
			Chunk ch=null;
			if (ss.equals("${pageNumber}")){
				PdfWriter writer = getWriter();
				if (writer!=null) ss=writer.getPageNumber()+""; else ss=ThreadHelper.get("pageNumber")+"";
			}
			else
			if (ss.equals("${pageCount}")){
				ss=ThreadHelper.get("pageCount")+"";
			}
			else
			if (ss.equals("${newPage}")){
				ch=Chunk.NEXTPAGE;
			}
			else
			if (ss.equals("${newLine}")){
				ch=Chunk.NEWLINE;
			}
			if (ch==null)
			ch=new Chunk(ss,new Font(base));
			applyFont(ch,it);
			
			String gt=MapUtil.getStr(it,"goto", null);
			if (gt!=null){
				ch.setLocalGoto(gt);
			}
			
			p.add(ch);
		}
	}
	
	private void applyFont(Chunk ch, Map it) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		ch.getFont().setSize(getSize(it));
		ch.getFont().setStyle(getStyle(it));
		ch.getFont().setColor(getColor(it,"color"));
		MapUtil.setIfFloat(it,"textRise",ch);
		Float skewAlpha=MapUtil.getFloat(it,"skewAlpha",null);
		Float skewBeta=MapUtil.getFloat(it,"skewBeta",null);
		if (skewAlpha!=null && skewBeta!=null) ch.setSkew(skewAlpha.floatValue(), skewBeta.floatValue());
		MapUtil.setIfStr(it,"localDestination",ch);
		MapUtil.setIfStr(it,"localGoto",ch);
	}
	
	private int getSize(Map it){
		Integer sz=MapUtil.getInt(it, "fontSize", (Integer)ThreadHelper.get("_defaultFontSize_"));
		if (sz!=null) return sz;
		return -1;
	}
	
	private BaseColor getColor(Map row, String id) {
		String color=(String)row.get(id);
		if (color==null) return null;
		Color c=Color.getColor(color);
		if (c==null) return null;
		return new BaseColor(c.getRed(),c.getBlue(),c.getGreen());
	}
	
	private int getStyle(Map row) {
		Boolean bold=(Boolean)row.get("bold");
		Boolean italic=(Boolean)row.get("italic");
		Boolean underline=(Boolean)row.get("underline");
		Boolean strike=(Boolean)row.get("strike");
		Boolean normal=(Boolean)row.get("normal");
		
		int style=0;
		if (bold!=null && bold) style|=1;
		if (italic!=null && italic) style|=2;
		if (underline!=null && underline) style|=4;
		if (strike!=null && strike) style|=8;
		if (style==0 && (normal==null || normal==false)) style=-1;
		return style;
	}
	
	private Image getImage(Map row) throws Exception{
		Image image=null;
		String url=(String)row.get("url");
		if (url!=null){
			image=Image.getInstance(url);
		}
		String externalId=(String)row.get("externalId");
		if (externalId!=null){
			File tmp=File.createTempFile("tmp", ".image");
			InputStream is = getFs().getResource(externalId);
			FileOutputStream os = new FileOutputStream(tmp);
			StreamUtil.copyStream(is, os, 0);
			is.close();
			os.close();
			image=Image.getInstance(tmp.toURL());
			ThreadHelper.postponeDelete(tmp);
		}
		String img=(String)row.get("image");
		if (img!=null){
			byte[] bytes=LightStr.getHexContent(img);
			image=Image.getInstance(bytes);
		}
		if (image!=null){
			MapUtil.setIfFloat(row, "spacingAfter", image);
			MapUtil.setIfFloat(row, "spacingBefore", image);
			MapUtil.setIfFloat(row, "scalePercent", image);
			MapUtil.setIfFloat(row, "scaleDegree", image);
			Float fitPer=MapUtil.getFloat(row, "fitPercent", null);
			if (fitPer!=null){
				Document doc = this.getDoc();
				image.scaleToFit((doc.getPageSize().getWidth()-doc.leftMargin()-doc.rightMargin())*fitPer/100f,(doc.getPageSize().getHeight()-doc.topMargin()-doc.bottomMargin())*fitPer/100f);
			}
			Float fitWidth=MapUtil.getFloat(row, "fitWidth", null);
			if (fitWidth!=null){
				Float fitHeight=MapUtil.getFloat(row, "fitHeight", fitWidth);
				image.scaleToFit(fitWidth,fitHeight);
			}
			image.setAlignment(getAlignment(row,"alignment"));
		}
		return image;
	}

	private com.itextpdf.text.List getList(Map row) throws Exception{
		String type=(String)row.get("type");
		com.itextpdf.text.List list=null;
		if ("greek".equals(type)) list=new GreekList();
		else
		if ("roman".equals(type)) list=new RomanList();
		else
		if ("zd".equals(type)) list=new ZapfDingbatsList(1);
		else
		if ("zd#".equals(type)) list=new ZapfDingbatsNumberList(1);
		else list= new com.itextpdf.text.List();
		
		Integer charNumber=(Integer)row.get("charNumber");
		if (charNumber!=null){
			if (list instanceof ZapfDingbatsList){
				((ZapfDingbatsList)list).setCharNumber(charNumber);
			}
			if (list instanceof ZapfDingbatsNumberList){
				((ZapfDingbatsNumberList)list).setType(charNumber);
			}
		}
		Boolean lowercase=(Boolean)row.get("lowercase");
		if (lowercase!=null) list.setLowercase(lowercase);
		Boolean numbered=(Boolean)row.get("numbered");
        if (numbered!=null) list.setNumbered(numbered);
        Boolean lettered=(Boolean)row.get("lettered");
        if (lettered!=null) list.setLettered(lettered);
        Integer first=(Integer)row.get("first");
        if (first!=null) list.setFirst(first);
        String symbol=(String)row.get("symbol");
        if (symbol!=null) {
        	Paragraph p=new Paragraph();
        	getChunks(p,row,symbol);
        	if (p.size()>0){
        		Object sym = p.get(0);
        		if (sym instanceof Chunk){
            		list.setListSymbol((Chunk)sym);
        		}
        	}
        }
        Float ident=(Float)row.get("ident"); if (ident==null) ident=10f;
        list.setSymbolIndent(ident);
        List<Map> lst=(List)row.get("items");
        if (lst!=null){
        	for (Map item:lst){
        		Element el=getElement(item);
        		if (el!=null){
        			ListItem li=new ListItem();
            		li.add(el);
            		list.add(li);
        		}
        	}
        }
        if (list.size()>0){
        	Object o = list.getItems().get(0);
        	if (o instanceof Paragraph){
        		Paragraph p=(Paragraph)o;
        		Float spaceBefore=(Float)row.get("spaceBefore");
        		if (spaceBefore!=null) p.setSpacingBefore(spaceBefore);
        	}
        	o = list.getItems().get(list.getItems().size()-1);
        	if (o instanceof Paragraph){
        		Paragraph p=(Paragraph)o;
        		Float spaceAfter=(Float)row.get("spaceAfter");
        		if (spaceAfter!=null) p.setSpacingAfter(spaceAfter);
        	}
        }
		return list;
	}
	
	
	
	private BaseFont getBaseFont(String id,String name, String encoding){
		BaseFont font=getFonts().get(id);
		if (font==null){
			try {
				font=BaseFont.createFont(name, encoding,BaseFont.NOT_EMBEDDED);
			} catch (Exception e) {
				e.printStackTrace();
				font=getDefaultFont();
			}
			getFonts().put(id,font);
		}
		return font;
	}
	
	private BaseFont getZhFont(){
		return getBaseFont("zh","STSong-Light","UniGB-UCS2-H");
	}
	
	private BaseFont getJpFont(){
		return getBaseFont("jp","KozMinPro-Regular", "UniJIS-UCS2-H");
	}
	
	private BaseFont getKsFont(){
		return getBaseFont("ks","HYGoThic-Medium", "UniKS-UCS2-H");
	}
	
	private boolean compatible(List<BaseFont> base,char c){
		BaseFont font;
		if (c>=0x4e00 && c<=0x9fa5) {
			if (base.size()==0){
				base.add(getZhFont());
				base.add(getJpFont());
				base.add(getKsFont());
				return true;
			}
			else{
				if (base.indexOf(getZhFont())>=0) return true; 
				if (base.indexOf(getJpFont())>=0) return true;
				if (base.indexOf(getKsFont())>=0) return true;
				return false;
			}
		}
		if (c>=0x30a0 && c<=0x30ff){
			if (base.size()==0){
				base.add(getJpFont());
				return true;
			}
			if (base.indexOf(getJpFont())>=0){
				if (base.size()==1) return true;
				base.clear();
				base.add(getJpFont());
				return true;
			}
			return false;
		}
		if (c>=0xac00 && c<=0xd7af){
			if (base.size()==0){
				base.add(getKsFont());
				return true;
			}
			if (base.indexOf(getKsFont())>=0){
				if (base.size()==1) return true;
				base.clear();
				base.add(getKsFont());
				return true;
			}
			return false;
		}
		if (base.size()==0){
			base.add(getDefaultFont());
		}
		return true;
	}
	
	public Image getBarcode(Map it) throws Exception{
		String type=MapUtil.getStr(it,"type");
		String code=MapUtil.getStr(it,"code");
		switch (type){
		case "pf417":
			BarcodePDF417 bar=new BarcodePDF417();
			bar.setText(code);
			return bar.getImage();
		case "QRCode":
			BarcodeQRCode qr=new BarcodeQRCode(code,MapUtil.getInt(it,"qrWidth",1),MapUtil.getInt(it,"qrHeight",1),null);
			return qr.getImage();
		default:
			Barcode barcode;
			PdfContentByte cb = getWriter().getDirectContent();
			switch(type){
			case "code128":
			case "code128_raw":
				barcode=new Barcode128();
				barcode.setCodeType(Barcode.CODE128_RAW);
				break;
			case "code128_ucc":
				barcode=new Barcode128();
				barcode.setCodeType(Barcode.CODE128_UCC);
				break;
			case "inter25":
				barcode=new BarcodeInter25();
				break;
			case "postnet":
				barcode=new BarcodePostnet();
				break;
			case "planet":
				barcode=new BarcodePostnet();
				barcode.setCodeType(Barcode.PLANET);
				break;
			case "code39":
				barcode=new Barcode39();
				break;
			case "codabar":
				barcode=new BarcodeCodabar();
				break;
			default:
				barcode = new BarcodeEAN();
				MapUtil.setIfBool(it,"guardBars",barcode,"setGuardBars");
				MapUtil.setIfFloat(it,"baseLine",barcode,"setBaseLine");
				if ("upca".equals(type)) barcode.setCodeType(Barcode.UPCA);
				if ("ean8".equals(type)) barcode.setCodeType(Barcode.EAN8);
				if ("upce".equals(type)) barcode.setCodeType(Barcode.UPCE);
				if ("ean13".equals(type)) barcode.setCodeType(Barcode.EAN13);
			}
			barcode.setCode(code);
			MapUtil.setIfFloat(it, "barHeight", barcode, "setBarHeight");
			MapUtil.setIfFloat(it, "x", barcode, "setX");
			MapUtil.setIfFloat(it, "n", barcode, "setN");
			MapUtil.setIfFloat(it, "size", barcode, "setSize");
			barcode.setTextAlignment(getAlignment(it,"alignment"));
			MapUtil.setIfBool(it, "checksumText", barcode, "setChecksumText");
			MapUtil.setIfBool(it, "startStopText", barcode, "setStartStopText");
			MapUtil.setIfBool(it, "extended", barcode, "setExtended");
			String suppCode=MapUtil.getStr(it, "suppCode");
			if (!LightStr.isEmpty(suppCode)){
				BarcodeEAN codeSUPP = new BarcodeEAN();
				codeSUPP.setCodeType(Barcode.SUPP5);
				codeSUPP.setCode(suppCode);
				codeSUPP.setBaseline(-2);
				BarcodeEANSUPP eanSupp = new BarcodeEANSUPP(barcode, codeSUPP);
				return eanSupp.createImageWithBarcode(cb, getColor(it,"barColor"), getColor(it,"textColor"));
			}
			else{
				return barcode.createImageWithBarcode(cb, getColor(it,"barColor"), getColor(it,"textColor"));	
			}
		}
	}
	
	private PdfPTable getTable(Map it) throws Exception{
		String ss=MapUtil.getStr(it,"widthList","100");
		List<String> st=MapUtil.getSelectList(ss);
		float[] widths=new float[st.size()];
		for (int i=0;i<st.size();i++) widths[i]=LightUtil.decodeFloat(st.get(i));
		PdfPTable table=new PdfPTable(widths);
		table.setExtendLastRow(false, false);
		MapUtil.setIfFloat(it,"spacingAfter", table);
		MapUtil.setIfFloat(it,"spacingBefore", table);
		MapUtil.setIfInt(it,"headerRows", table);
		MapUtil.setIfInt(it,"footerRows", table);
		MapUtil.setIfBool(it,"skipFirstHeader", table);
		MapUtil.setIfBool(it,"skipLastFooter", table);
		MapUtil.setIfFloat(it, "widthPercentage", table);
		MapUtil.setIfBool(it,"splitRows", table);
		MapUtil.setIfBool(it,"splitLate", table);
		MapUtil.setIfBool(it,"extendLastRow", table);
		MapUtil.setIfBool(it,"keepTogether", table);
		List<Map> items=(List)it.get("items");
		if (items!=null){
			for (Map cell:items){
				Element el=getElement(cell);

				PdfPCell pc = new PdfPCell();
				if (el!=null)
				if (el instanceof Phrase) pc=new PdfPCell((Phrase)el);
				else
				if (el instanceof Image) pc=new PdfPCell((Image)el);
				else{
					pc.addElement(el);
				}
				pc.setHorizontalAlignment(getAlignment(cell,"align"));
				pc.setVerticalAlignment(getVAlign(cell,"valign"));
				pc.setUseBorderPadding(true);
				MapUtil.setIfInt(cell, "rowspan", pc);
				MapUtil.setIfInt(cell, "colspan", pc);
				MapUtil.setIfInt(cell, "border", pc);
				MapUtil.setIfInt(cell, "rotation", pc);
				MapUtil.setIfFloat(cell, "borderWidth", pc);

				BaseColor color=getColor(cell,"bgColor");
				if (color!=null) pc.setBackgroundColor(color);
				table.addCell(pc);	
			}
		}
		return table;
	}
	
	private void getDirectContent(PdfContentByte cb, Rectangle ps, Map it) throws Exception{
		BaseColor color=getColor(it,"fillColor");
		if (color!=null) cb.setColorFill(color);
		
		float x=MapUtil.getFloat(it, "x",0f);
		float y=MapUtil.getFloat(it, "y",0f);
		float w=MapUtil.getFloat(it, "w",0f);
		float h=MapUtil.getFloat(it, "h",0f);
		
		float xPer=MapUtil.getFloat(it, "xPer",0f);
		float yPer=MapUtil.getFloat(it, "yPer",0f);
		float wPer=MapUtil.getFloat(it, "wPer",0f);
		float hPer=MapUtil.getFloat(it, "hPer",0f);
		
		String pos=MapUtil.getStr(it, "position","bottom");
		switch (pos){
		case "top": y+=ps.getHeight(); break;
		case "right": x+=ps.getWidth(); break;
		}
		
		float xx=x+ps.getWidth()*xPer/100f;
		float yy=y+ps.getWidth()*yPer/100f;
		float ww=ps.getWidth()*wPer/100f+w;
		float hh=ps.getHeight()*hPer/100f+h;
		
		int font=MapUtil.getInt(it,"fontSize", 8);
		cb.setFontAndSize(getDefaultFont(), font);
		
		cb.beginText();
		
		String cls=MapUtil.getStr(it, "cls","");
			
		if (cls.equals("image")){
			Image img=getImage(it);
			cb.addImage(img,img.getWidth(),0,0,img.getHeight(),xx,yy);
		}
		else{
			String text=LightUtil.macro(MapUtil.getStr(it, "text",""),'$').toString();
			float degree=MapUtil.getFloat(it, "rotateDegree", 0f);
			boolean kerned=MapUtil.getBool(it,"kerned",false);
			int align = getAlignment(it,"alignment");
			x=xx;y=yy;
			switch(align){
			case Element.ALIGN_CENTER:
				x=xx+ww/2;
				break;
			case Element.ALIGN_RIGHT:
				x=xx+ww;
				break;
			default:
				align=Element.ALIGN_LEFT;
				break;
			}
			if (kerned) 
				cb.showTextAlignedKerned(align, text, x,y, degree); 
			else 
				cb.showTextAligned(align, text, x,y, degree);
		}
		
		cb.endText();
	}
}

/*
 * $Id: HTMLWorker.java 5075 2012-02-27 16:36:18Z blowagie $
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 1998-2012 1T3XT BVBA
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY 1T3XT,
 * 1T3XT DISCLAIMS THE WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */

class HTMLCoWorker extends HTMLWorker{

	private static Logger LOGGER = LoggerFactory.getLogger(HTMLWorker.class);
	
	/** The object defining all the styles. */
	private StyleSheet style = new StyleSheet();

	/**
	 * Creates a new instance of HTMLWorker
	 * @param document A class that implements <CODE>DocListener</CODE>
	 */
	public HTMLCoWorker(final DocListener document) {
		this(document, null, null);
	}

	/**
	 * Creates a new instance of HTMLWorker
	 * @param document	A class that implements <CODE>DocListener</CODE>
	 * @param tags		A map containing the supported tags
	 * @param style		A StyleSheet
	 * @since 5.0.6
	 */
	public HTMLCoWorker(final DocListener document, final Map<String, HTMLTagProcessor> tags, final StyleSheet style) {
		super(document, tags, style);
		setStyleSheet(style);
		
	}

	/**
	 * Setter for the StyleSheet
	 * @param style the StyleSheet
	 */
	public void setStyleSheet(StyleSheet style) {
		if (style == null)
			style = new StyleSheet();
		this.style = style;
	}

	/**
	 * The current hierarchy chain of tags.
	 * @since 5.0.6
	 */
	private final ChainedProperties chain = new ChainedProperties();

	/**
	 * @see com.itextpdf.text.xml.simpleparser.SimpleXMLDocHandler#startDocument()
	 */
	public void startDocument() {
		HashMap<String, String> attrs = new HashMap<String, String>();
		style.applyStyle(HtmlTags.BODY, attrs);
		chain.addToChain(HtmlTags.BODY, attrs);
	}

    /**
     * @see com.itextpdf.text.xml.simpleparser.SimpleXMLDocHandler#startElement(java.lang.String, java.util.Map)
     */
    public void startElement(final String tag, final Map<String, String> attrs) {
		HTMLTagProcessor htmlTag = tags.get(tag);
		if (htmlTag == null) {
			return;
		}
		// apply the styles to attrs
		style.applyStyle(tag, attrs);
		// deal with the style attribute
		StyleSheet.resolveStyleAttribute(attrs, chain);
		// process the tag
		try {
			htmlTag.startElement(this, tag, attrs);
		} catch (DocumentException e) {
			throw new ExceptionConverter(e);
		} catch (IOException e) {
			throw new ExceptionConverter(e);
		}
	}

	/**
	 * @see com.itextpdf.text.xml.simpleparser.SimpleXMLDocHandler#text(java.lang.String)
	 */
	public void text(String content) {
		if (skipText)
			return;
		if (currentParagraph == null) {
			currentParagraph = createParagraph();
		}
		if (!insidePRE) {
			// newlines and carriage returns are ignored
			if (content.trim().length() == 0 && content.indexOf(' ') < 0) {
				return;
			}
			content = HtmlUtilities.eliminateWhiteSpace(content);
		}
		Chunk chunk = createChunk(content);
		currentParagraph.add(chunk);
	}

	/**
	 * @see com.itextpdf.text.xml.simpleparser.SimpleXMLDocHandler#endElement(java.lang.String)
	 */
	public void endElement(final String tag) {
		HTMLTagProcessor htmlTag = tags.get(tag);
		if (htmlTag == null) {
			return;
		}
		// process the tag
		try {
			htmlTag.endElement(this, tag);
		} catch (DocumentException e) {
			throw new ExceptionConverter(e);
		}
	}

	/**
	 * @see com.itextpdf.text.xml.simpleparser.SimpleXMLDocHandler#endDocument()
	 */
	public void endDocument() {
		try {
			// flush the stack
			for (int k = 0; k < stack.size(); ++k)
				document.add(stack.elementAt(k));
			// add current paragraph
			if (currentParagraph != null)
				document.add(currentParagraph);
			currentParagraph = null;
		} catch (Exception e) {
			throw new ExceptionConverter(e);
		}
	}

	// stack and current paragraph operations

	/**
	 * Adds a new line to the currentParagraph.
	 * @since 5.0.6
	 */
	public void newLine() {
		if (currentParagraph == null) {
			currentParagraph = new Paragraph();
		}
		currentParagraph.add(createChunk("\n"));
	}

	/**
	 * Flushes the current paragraph, indicating that we're starting
	 * a new block.
	 * If the stack is empty, the paragraph is added to the document.
	 * Otherwise the Paragraph is added to the stack.
	 * @since 5.0.6
	 */
	public void carriageReturn() throws DocumentException {
		if (currentParagraph == null)
			return;
		if (stack.empty())
			document.add(currentParagraph);
		else {
			Element obj = stack.pop();
			if (obj instanceof TextElementArray) {
				TextElementArray current = (TextElementArray) obj;
				current.add(currentParagraph);
			}
			stack.push(obj);
		}
		currentParagraph = null;
	}

	/**
	 * Stacks the current paragraph, indicating that we're starting
	 * a new span.
	 * @since 5.0.6
	 */
	public void flushContent() {
		pushToStack(currentParagraph);
		currentParagraph = new Paragraph();
	}

	/**
	 * Pushes an element to the Stack.
	 * @param element
	 * @since 5.0.6
	 */
	public void pushToStack(final Element element) {
		if (element != null)
			stack.push(element);
	}

	/**
	 * Updates the chain with a new tag and new attributes.
	 * @param tag	the new tag
	 * @param attrs	the corresponding attributes
	 * @since 5.0.6
	 */
	public void updateChain(final String tag, final Map<String, String> attrs) {
		chain.addToChain(tag, attrs);
	}

	/**
	 * Updates the chain by removing a tag.
	 * @param tag	the new tag
	 * @since 5.0.6
	 */
	public void updateChain(final String tag) {
		chain.removeChain(tag);
	}

	/**
	 * Map containing providers such as a FontProvider or ImageProvider.
	 * @since 5.0.6 (renamed from interfaceProps)
	 */
	private Map<String, Object> providers = new HashMap<String, Object>();

	/**
	 * Setter for the providers.
	 * If a FontProvider is added, the ElementFactory is updated.
	 * @param providers a Map with different providers
	 * @since 5.0.6
	 */
	public void setProviders(final Map<String, Object> providers) {
		if (providers == null)
			return;
		this.providers = providers;
		FontProvider ff = null;
		if (providers != null)
			ff = (FontProvider) providers.get(FONT_PROVIDER);
		if (ff != null)
			factory.setFontProvider(ff);
	}

	// factory that helps create objects

	/**
	 * Factory that is able to create iText Element objects.
	 * @since 5.0.6
	 */
	private ElementFactory factory = new ElementFactory();

	/**
	 * Creates a Chunk using the factory.
	 * @param content	the content of the chunk
	 * @return	a Chunk with content
	 * @since 5.0.6
	 */
	public Chunk createChunk(final String content) {
		return factory.createChunk(content, chain);
	}
	/**
	 * Creates a Paragraph using the factory.
	 * @return	a Paragraph without any content
	 * @since 5.0.6
	 */
	public Paragraph createParagraph() {
		return factory.createParagraph(chain);
	}
	/**
	 * Creates a List object.
	 * @param tag should be "ol" or "ul"
	 * @return	a List object
	 * @since 5.0.6
	 */
	public com.itextpdf.text.List createList(final String tag) {
		return factory.createList(tag, chain);
	}
	/**
	 * Creates a ListItem object.
	 * @return a ListItem object
	 * @since 5.0.6
	 */
	public ListItem createListItem() {
		return factory.createListItem(chain);
	}
	/**
	 * Creates a LineSeparator object.
	 * @param attrs	properties of the LineSeparator
	 * @return a LineSeparator object
	 * @since 5.0.6
	 */
	public LineSeparator createLineSeparator(final Map<String, String> attrs) {
		return factory.createLineSeparator(attrs, currentParagraph.getLeading()/2);
	}

	/**
	 * Creates an Image object.
	 * @param attrs properties of the Image
	 * @return an Image object (or null if the Image couldn't be found)
	 * @throws DocumentException
	 * @throws IOException
	 * @since 5.0.6
	 */
	public Image createImage(final Map<String, String> attrs) throws DocumentException, IOException {
		String src = attrs.get(HtmlTags.SRC);
		if (src == null)
			return null;
		Image img = factory.createImage(
				src, attrs, chain, document,
				(ImageProvider)providers.get(IMG_PROVIDER),
				(ImageStore)providers.get(IMG_STORE),
				(String)providers.get(IMG_BASEURL));
		return img;
	}

	/**
	 * Creates a Cell.
	 * @param tag	the tag
	 * @return	a CellWrapper object
	 * @since 5.0.6
	 */
	public CellWrapper createCell(final String tag) {
		return new CellWrapper(tag, chain);
	}

	// processing objects

	/**
	 * Adds a link to the current paragraph.
	 * @since 5.0.6
	 */
	public void processLink() {
		if (currentParagraph == null) {
			currentParagraph = new Paragraph();
		}
		// The link provider allows you to do additional processing
		LinkProcessor i = (LinkProcessor) providers.get(HTMLWorker.LINK_PROVIDER);
		if (i == null || !i.process(currentParagraph, chain)) {
			// sets an Anchor for all the Chunks in the current paragraph
			String href = chain.getProperty(HtmlTags.HREF);
			if (href != null) {
				for (Chunk ck : currentParagraph.getChunks()) {
					ck.setAnchor(href);
				}
			}
		}
		// a link should be added to the current paragraph as a phrase
		if (stack.isEmpty()) {
			// no paragraph to add too, 'a' tag is first element
			Paragraph tmp = new Paragraph(new Phrase(currentParagraph));
			currentParagraph = tmp;
		} else {
			Paragraph tmp = (Paragraph) stack.pop();
			tmp.add(new Phrase(currentParagraph));
			currentParagraph = tmp;
		}
	}

	/**
	 * Fetches the List from the Stack and adds it to
	 * the TextElementArray on top of the Stack,
	 * or to the Document if the Stack is empty.
	 * @throws DocumentException
	 * @since 5.0.6
	 */
	public void processList() throws DocumentException {
		if (stack.empty())
			return;
		Element obj = stack.pop();
		if (!(obj instanceof com.itextpdf.text.List)) {
			stack.push(obj);
			return;
		}
		if (stack.empty())
			document.add(obj);
		else
			((TextElementArray) stack.peek()).add(obj);
	}

	/**
	 * Looks for the List object on the Stack,
	 * and adds the ListItem to the List.
	 * @throws DocumentException
	 * @since 5.0.6
	 */
	public void processListItem() throws DocumentException {
		if (stack.empty())
			return;
		Element obj = stack.pop();
		if (!(obj instanceof ListItem)) {
			stack.push(obj);
			return;
		}
		if (stack.empty()) {
			document.add(obj);
			return;
		}
		ListItem item = (ListItem) obj;
		Element list = stack.pop();
		if (!(list instanceof com.itextpdf.text.List)) {
			stack.push(list);
			return;
		}
		((com.itextpdf.text.List) list).add(item);
		item.adjustListSymbolFont();
		stack.push(list);
	}

	/**
	 * Processes an Image.
	 * @param img
	 * @param attrs
	 * @throws DocumentException
	 * @since	5.0.6
	 */
	public void processImage(final Image img, final Map<String, String> attrs) throws DocumentException {
		ImageProcessor processor = (ImageProcessor)providers.get(HTMLWorker.IMG_PROCESSOR);
		if (processor == null || !processor.process(img, attrs, chain, document)) {
			String align = attrs.get(HtmlTags.ALIGN);
			if (align != null) {
				carriageReturn();
			}
			if (currentParagraph == null) {
				currentParagraph = createParagraph();
			}
			currentParagraph.add(new Chunk(img, 0, 0, true));
			currentParagraph.setAlignment(HtmlUtilities.alignmentValue(align));
			if (align != null) {
				carriageReturn();
			}
		}
	}

	/**
	 * Processes the Table.
	 * @throws DocumentException
	 * @since 5.0.6
	 */
	public void processTable() throws DocumentException{
		TableWrapper table = (TableWrapper) stack.pop();
		PdfPTable tb = table.createTable();
		tb.setSplitRows(true);
		if (stack.empty())
			document.add(tb);
		else
			((TextElementArray) stack.peek()).add(tb);
	}

	/**
	 * Gets the TableWrapper from the Stack and adds a new row.
	 * @since 5.0.6
	 */
	public void processRow() {
		ArrayList<PdfPCell> row = new ArrayList<PdfPCell>();
        ArrayList<Float> cellWidths = new ArrayList<Float>();
        boolean percentage = false;
        float width;
        float totalWidth = 0;
        int zeroWidth = 0;
		TableWrapper table = null;
		while (true) {
			Element obj = stack.pop();
			if (obj instanceof CellWrapper) {
                CellWrapper cell = (CellWrapper)obj;
                width = cell.getWidth();
                cellWidths.add(new Float(width));
                percentage |= cell.isPercentage();
                if (width == 0) {
                	zeroWidth++;
                }
                else {
                	totalWidth += width;
                }
                row.add(cell.getCell());
			}
			if (obj instanceof TableWrapper) {
				table = (TableWrapper) obj;
				break;
			}
		}
        table.addRow(row);
        if (cellWidths.size() > 0) {
            // cells come off the stack in reverse, naturally
        	totalWidth = 100 - totalWidth;
            Collections.reverse(cellWidths);
            float[] widths = new float[cellWidths.size()];
            boolean hasZero = false;
            for (int i = 0; i < widths.length; i++) {
                widths[i] = cellWidths.get(i).floatValue();
                if (widths[i] == 0 && percentage && zeroWidth > 0) {
                	widths[i] = totalWidth / zeroWidth;
                }
                if (widths[i] == 0) {
                    hasZero = true;
                    break;
                }
            }
            if (!hasZero)
                table.setColWidths(widths);
        }
		stack.push(table);
	}

	// state variables and methods

	/** Stack to keep track of table tags. */
	private final Stack<boolean[]> tableState = new Stack<boolean[]>();

	/** Boolean to keep track of TR tags. */
	private boolean pendingTR = false;

	/** Boolean to keep track of TD and TH tags */
	private boolean pendingTD = false;

	/** Boolean to keep track of LI tags */
	private boolean pendingLI = false;

	/**
	 * Boolean to keep track of PRE tags
	 * @since 5.0.6 renamed from isPRE
	 */
	private boolean insidePRE = false;

	/**
	 * Pushes the values of pendingTR and pendingTD
	 * to a state stack.
	 * @since 5.0.6
	 */
	public void pushTableState() {
		tableState.push(new boolean[] { pendingTR, pendingTD });
	}

	/**
	 * Pops the values of pendingTR and pendingTD
	 * from a state stack.
	 * @since 5.0.6
	 */
	public void popTableState() {
		boolean[] state = tableState.pop();
		pendingTR = state[0];
		pendingTD = state[1];
	}

	/**
	 * @return the pendingTR
	 * @since 5.0.6
	 */
	public boolean isPendingTR() {
		return pendingTR;
	}

	/**
	 * @param pendingTR the pendingTR to set
	 * @since 5.0.6
	 */
	public void setPendingTR(final boolean pendingTR) {
		this.pendingTR = pendingTR;
	}

	/**
	 * @return the pendingTD
	 * @since 5.0.6
	 */
	public boolean isPendingTD() {
		return pendingTD;
	}

	/**
	 * @param pendingTD the pendingTD to set
	 * @since 5.0.6
	 */
	public void setPendingTD(final boolean pendingTD) {
		this.pendingTD = pendingTD;
	}

	/**
	 * @return the pendingLI
	 * @since 5.0.6
	 */
	public boolean isPendingLI() {
		return pendingLI;
	}

	/**
	 * @param pendingLI the pendingLI to set
	 * @since 5.0.6
	 */
	public void setPendingLI(final boolean pendingLI) {
		this.pendingLI = pendingLI;
	}

	/**
	 * @return the insidePRE
	 * @since 5.0.6
	 */
	public boolean isInsidePRE() {
		return insidePRE;
	}

	/**
	 * @param insidePRE the insidePRE to set
	 * @since 5.0.6
	 */
	public void setInsidePRE(final boolean insidePRE) {
		this.insidePRE = insidePRE;
	}

	/**
	 * @return the skipText
	 * @since 5.0.6
	 */
	public boolean isSkipText() {
		return skipText;
	}

	/**
	 * @param skipText the skipText to set
	 * @since 5.0.6
	 */
	public void setSkipText(final boolean skipText) {
		this.skipText = skipText;
	}


	// DocListener interface

	/**
	 * @see com.itextpdf.text.ElementListener#add(com.itextpdf.text.Element)
	 */
	public boolean add(final Element element) throws DocumentException {
		objectList.add(element);
		return true;
	}

	/**
	 * @see com.itextpdf.text.DocListener#close()
	 */
	public void close() {
	}

	/**
	 * @see com.itextpdf.text.DocListener#newPage()
	 */
	public boolean newPage() {
		return true;
	}

	/**
	 * @see com.itextpdf.text.DocListener#open()
	 */
	public void open() {
	}

	/**
	 * @see com.itextpdf.text.DocListener#resetPageCount()
	 */
	public void resetPageCount() {
	}

	/**
	 * @see com.itextpdf.text.DocListener#setMarginMirroring(boolean)
	 */
	public boolean setMarginMirroring(final boolean marginMirroring) {
		return false;
	}

	/**
     * @see com.itextpdf.text.DocListener#setMarginMirroring(boolean)
	 * @since	2.1.6
	 */
	public boolean setMarginMirroringTopBottom(final boolean marginMirroring) {
		return false;
	}

	/**
	 * @see com.itextpdf.text.DocListener#setMargins(float, float, float, float)
	 */
	public boolean setMargins(final float marginLeft, final float marginRight,
			final float marginTop, final float marginBottom) {
		return true;
	}

	/**
	 * @see com.itextpdf.text.DocListener#setPageCount(int)
	 */
	public void setPageCount(final int pageN) {
	}

	/**
	 * @see com.itextpdf.text.DocListener#setPageSize(com.itextpdf.text.Rectangle)
	 */
	public boolean setPageSize(final Rectangle pageSize) {
		return true;
	}

	// deprecated methods

	/**
	 * Sets the providers.
	 * @deprecated use setProviders() instead
	 */
	@Deprecated
	public void setInterfaceProps(final HashMap<String, Object> providers) {
		setProviders(providers);
	}
	/**
	 * Gets the providers
	 * @deprecated use getProviders() instead
	 */
	@Deprecated
	public Map<String, Object> getInterfaceProps() {
		return providers;
	}

	public ElementFactory getFactory() {
		return factory;
	}

	public void setFactory(ElementFactory factory) {
		this.factory = factory;
	}

}

class DirectHtmlWorker extends HTMLCoWorker {

	public DirectHtmlWorker(DocListener document) {
		super(document);
		setFactory(new DirectElementFactory());
	}
	
	public DirectHtmlWorker(DocListener document,
			Map<String, HTMLTagProcessor> tags, StyleSheet style) {
		super(document, tags, style);
		setFactory(new DirectElementFactory());
	}

	public static List<Element> parse2List(final Reader reader, final StyleSheet style,
			final Map<String, HTMLTagProcessor> tags, final HashMap<String, Object> providers) throws IOException {
		DirectHtmlWorker worker = new DirectHtmlWorker(null, tags, style);
		worker.setFactory(new DirectElementFactory());
		worker.document = worker;
		worker.setProviders(providers);
		worker.objectList = new ArrayList<Element>();
		worker.parse(reader);
		return worker.objectList;
	}
}

class DirectElementFactory extends ElementFactory {
	@Override
	public Image createImage(
			String src,
			final Map<String, String> attrs,
			final ChainedProperties chain,
			final DocListener document,
			final ImageProvider img_provider,
			final HashMap<String, Image> img_store,
			final String img_baseurl) throws DocumentException, IOException {
		Image img = null;
		// getting the image using an image provider
		if (img_provider != null)
			img = img_provider.getImage(src, attrs, chain, document);
		// getting the image from an image store
		if (img == null && img_store != null) {
			Image tim = img_store.get(src);
			if (tim != null)
				img = Image.getInstance(tim);
		}
		if (img != null)
			return img;
		////if src start with data: it's dataUri and parse it imme.
		if (src.startsWith("remote?")){
			BeanFactory bf = BeanFactory.getBeanFactory();
			String pp=src.substring(7);
			String[] ss=pp.split("\\&");
			try{
				String id="~",fsId=LightUtil.getRepository().getFsId();
				for (String s:ss){
					String[] sss=s.split("=");
					if (sss[0].equals("id")) id=sss[1];
					if (sss[0].equals("fsId")) fsId=sss[1];
				}
				IRepository fs = bf.getRepository(fsId);

				InputStream is = fs.getResource(id);
				ByteArrayOutputStream os=new ByteArrayOutputStream();
				StreamUtil.copyStream(is, os, 0);
				is.close();
				os.close();
				img=Image.getInstance(os.toByteArray());
			}
			catch(Exception e){e.printStackTrace();}
		}
		else
		if (src.startsWith("data:")){
			int i=src.indexOf(",");
			byte[] bits=Base64.decode(src.substring(i+1));
			img=Image.getInstance(bits);
		}
		else{
			////
			// introducing a base url
			// relative src references only
			if (!src.startsWith("http") && img_baseurl != null) {
				src = img_baseurl + src;
			}
			else if (img == null && !src.startsWith("http")) {
				String path = chain.getProperty(HtmlTags.IMAGEPATH);
				if (path == null)
					path = "";
				src = new File(path, src).getPath();
			}
			img = Image.getInstance(src);	
		}
		
		if (img == null)
			return null;

		float actualFontSize = HtmlUtilities.parseLength(
			chain.getProperty(HtmlTags.SIZE),
			HtmlUtilities.DEFAULT_FONT_SIZE);
		if (actualFontSize <= 0f)
			actualFontSize = HtmlUtilities.DEFAULT_FONT_SIZE;
		String width = attrs.get(HtmlTags.WIDTH);
		float widthInPoints = HtmlUtilities.parseLength(width, actualFontSize);
		String height = attrs.get(HtmlTags.HEIGHT);

		float heightInPoints = HtmlUtilities.parseLength(height, actualFontSize);
		
		if (widthInPoints ==0 && heightInPoints == 0){
			Document doc = (Document)document;
			widthInPoints=doc.getPageSize().getWidth();	
		}
		
		if (widthInPoints > 0 && heightInPoints > 0) {
			img.scaleAbsolute(widthInPoints, heightInPoints);
		} else if (widthInPoints > 0) {
			heightInPoints = img.getHeight() * widthInPoints
					/ img.getWidth();
			img.scaleAbsolute(widthInPoints, heightInPoints);
		} else if (heightInPoints > 0) {
			widthInPoints = img.getWidth() * heightInPoints
					/ img.getHeight();
			img.scaleAbsolute(widthInPoints, heightInPoints);
		}

		String before = chain.getProperty(HtmlTags.BEFORE);
		if (before != null)
			img.setSpacingBefore(Float.parseFloat(before));
		String after = chain.getProperty(HtmlTags.AFTER);
		if (after != null)
			img.setSpacingAfter(Float.parseFloat(after));
		img.setWidthPercentage(0);
		return img;
	}

}
