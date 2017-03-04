package com.masscustsoft.util;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

import java.lang.reflect.Method;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.masscustsoft.Lang.CLASS;
import com.masscustsoft.util.LightStr;

/**
 * Dynamic Class Generater Utility
 * 
 * @author JSong
 *
 */
public class AsmHelper {
	
	private ClassWriter cw;
	private String className;
	private MethodVisitor init; //constructor
	
	/**
	 * Constructor, must provide a base class which to extend. Assuming the base class have an empty constructor to override.
	 * 
	 * @param baseCls Base class to extend, if no base class, use Object.class instead
	 * @param tar Target class to generate, must be in full name with package name, Ex: com.masscustsoft.test.TestClass
	 */
	public AsmHelper(Class baseCls, String tar){
		String base=baseCls.getName().replace('.','/');
		className=tar.replace('.','/');
		if (tar.indexOf('.')==-1) className=base+CLASS.DIVIDER+tar;
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, base, new String[]{});
		//System.out.println("ASM: "+className+" extends "+base);
		init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		init.visitCode(); 
		init.visitVarInsn(ALOAD, 0); 
		init.visitMethodInsn(INVOKESPECIAL, base, "<init>", "()V");	
	}
	
	/**
	 * Current working ClassWriter.
	 */
	public ClassWriter getClassWriter(){
		return cw;
	}
	
	/**
	 * Add a field to the target class, automatically implemented getter and setter.
	 * 
	 * @param field The field name
	 * @param type The field type 
	 * @param def Default value or null
	 * @param annotations Annotations to the field. 
	 */
	public void addField(String field, Class type, Object def, Class[] annotations){
		String ftype="L"+type.getName().replace('.', '/')+";";
		
		FieldVisitor fw = cw.visitField(ACC_PRIVATE, field, ftype, null, null);
		if (annotations!=null) 
		{
			for (Class a:annotations){
				AnnotationVisitor aw = fw.visitAnnotation("L"+a.getName().replace('.','/')+";", true);
				aw.visitEnd();
			}
		}
		fw.visitEnd();
		
		MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "get"+LightStr.capitalize(field), "()"+ftype, null, null); 
		mw.visitCode(); 
		mw.visitVarInsn(ALOAD, 0); //this
		mw.visitFieldInsn(GETFIELD, className, field, ftype); 
		mw.visitInsn(ARETURN); 
		mw.visitMaxs(1, 1);
		mw.visitEnd();
		
		mw = cw.visitMethod(ACC_PUBLIC, "set"+LightStr.capitalize(field), "("+ftype+")V", null, null); 
		mw.visitCode(); 
		mw.visitVarInsn(ALOAD, 0); //this
		mw.visitVarInsn(ALOAD, 1); 
		mw.visitFieldInsn(PUTFIELD, className, field, ftype); 
		mw.visitInsn(RETURN); 
		mw.visitMaxs(2, 2); 
		mw.visitEnd();
		
		if (def!=null){
			init.visitVarInsn(ALOAD, 0);
			init.visitLdcInsn(def);
			//init.visitMethodInsn(INVOKESPECIAL, className, "set"+StrUtil.capitalize(field), "("+ftype+")V");
			init.visitFieldInsn(PUTFIELD, className, field, ftype); 
		}
	}

	private String getClassType(Class c, boolean single){
		String tp="";
		if (c.isPrimitive()){
			if (c.equals(void.class)) tp="V";
			if (c.equals(int.class)) tp="I";
			if (c.equals(long.class)) tp="J";
			if (c.equals(boolean.class)) tp="Z";
			if (c.equals(byte.class)) tp="B";
			if (c.equals(double.class)) tp="D";
			if (c.equals(float.class)) tp="F";
			if (c.equals(short.class)) tp="S";
		}
		else tp="L"+c.getName().replace('.', '/')+";";
		if (c.isArray()) tp="["+tp;
		return tp;
	}
	
	public void addProxyMethod(Method m){
		int mod=0;
		if ((m.getModifiers()&ACC_PUBLIC)!=0) mod=ACC_PUBLIC; else mod=ACC_PROTECTED;
		
		String sig="(";
		for (Class t:m.getParameterTypes()){
			
		}
		sig+=")"+m.getReturnType().getName().replace('.', '/');
		
		MethodVisitor mw = cw.visitMethod(mod, m.getName(), "()java/lang/String", null, null); 
		mw.visitCode(); 
		mw.visitVarInsn(ALOAD, 0); //this
		//mw.visitLdcInsn(ret);
		mw.visitInsn(ARETURN); 
		mw.visitMaxs(2, 2); 
		mw.visitEnd();
	}
	
	/**
	 * The Java byte code array generated for the target class.
	 */
	public byte[] toByteArray() {
		init.visitInsn(RETURN); 
		init.visitMaxs(0, 0);
		init.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}

	/**
	 * The target class name.
	 */
	public String getClassName() {
		return className;
	}
}
