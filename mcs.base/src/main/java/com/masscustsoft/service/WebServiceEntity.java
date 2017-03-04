package com.masscustsoft.service;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.masscustsoft.Lang.DynamicClassInfo;
import com.masscustsoft.api.IVariantConfig;
import com.masscustsoft.util.AsmHelper;
import com.masscustsoft.util.LightStr;
import com.masscustsoft.util.LightUtil;

public class WebServiceEntity implements IVariantConfig{
	/**
	 * The short name of a Entity, it should be a Class extended from Entity. 
	 */
	protected String name;
	
	/**
	 * Template for this Entity, default is {@link EntityVariantWrapper}
	 */
	protected String template="EntityVariantWrapper";
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
	
	/**
	 * The main method to generate a Web Service wrapper.
	 * 
	 * @param asm
	 * @param baseCls
	 * @param info
	 */
	public void genEntityService(AsmHelper asm, Class baseCls, DynamicClassInfo info) throws Exception {
		String base=baseCls.getName().replace('.','/');
		String entityName=getName();
		//System.out.println("enCls0="+entityName);
		String s1=asm.getClassName();
		int idx=s1.indexOf("/webservice/");
		String enCls=s1.substring(0,idx)+"/webservice/"+info.getCategoryId().toLowerCase()+"/"+entityName;
		String entityCls = LightUtil.getBeanFactory().findRealClass(entityName).replace('.', '/');
		String entityVar=LightStr.decapitalize(name);
		//System.out.println("enCls1="+enCls+", var="+entityVar);
		
		ClassWriter cw = asm.getClassWriter();

		//public void addEmployee(user,pass,Employee)
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "add"+LightStr.capitalize(entityName), "(Ljava/lang/String;Ljava/lang/String;L"+enCls+";)Ljava/lang/String;", null,new String[]{}); 
			mw.visitCode(); 
			Label l1=new Label();
			mw.visitLabel(l1);
			mw.visitVarInsn(ALOAD, 0); //this
		
			mw.visitVarInsn(ALOAD, 0); //this
			mw.visitVarInsn(ALOAD, 1); //user
			mw.visitVarInsn(ALOAD, 2); //pass
			mw.visitLdcInsn(Type.getType("L"+entityCls+";"));
			mw.visitVarInsn(ALOAD, 3); //wrapper
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, base, "addEntity", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/String;");
			
			mw.visitInsn(ARETURN); 
			Label l2=new Label();
			mw.visitLabel(l2);
			mw.visitLocalVariable("userId", "Ljava/lang/String;", null, l1,l2, 1);
			mw.visitLocalVariable("password", "Ljava/lang/String;", null, l1,l2, 2);
			mw.visitLocalVariable(entityVar, "L"+enCls+";", null, l1,l2, 3);
			
			mw.visitMaxs(0, 0); 
			mw.visitEnd();
		}
		
		//public void addEmployee(user,pass,Employee)
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "update"+LightStr.capitalize(entityName), "(Ljava/lang/String;Ljava/lang/String;L"+enCls+";)Ljava/lang/String;", null,new String[]{}); 
			mw.visitCode(); 
			Label l1=new Label();
			mw.visitLabel(l1);
			mw.visitVarInsn(ALOAD, 0); //this
			
			mw.visitVarInsn(ALOAD, 0); //this
			mw.visitVarInsn(ALOAD, 1); //user
			mw.visitVarInsn(ALOAD, 2); //pass
			mw.visitLdcInsn(Type.getType("L"+entityCls+";"));
			mw.visitVarInsn(ALOAD, 3); //wrapper
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, base, "updateEntity", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/String;");
			
			mw.visitInsn(ARETURN); 
			Label l2=new Label();
			mw.visitLabel(l2);
			mw.visitLocalVariable("userId", "Ljava/lang/String;", null, l1,l2, 1);
			mw.visitLocalVariable("password", "Ljava/lang/String;", null, l1,l2, 2);
			mw.visitLocalVariable(entityVar, "L"+enCls+";", null, l1,l2, 3);
			
			mw.visitMaxs(0, 0); 
			mw.visitEnd();
		}
		
		//public void deleteEmployee(user,pass,Employee)
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "delete"+LightStr.capitalize(entityName), "(Ljava/lang/String;Ljava/lang/String;L"+enCls+";)Ljava/lang/String;", null,new String[]{}); 
			mw.visitCode(); 
			Label l1=new Label();
			mw.visitLabel(l1);
			mw.visitVarInsn(ALOAD, 0); //this
			
			mw.visitVarInsn(ALOAD, 0); //this
			mw.visitVarInsn(ALOAD, 1); //user
			mw.visitVarInsn(ALOAD, 2); //pass
			mw.visitLdcInsn(Type.getType("L"+entityCls+";"));
			mw.visitVarInsn(ALOAD, 3); //wrapper
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, base, "deleteEntity", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/String;");
			
			mw.visitInsn(ARETURN); 
			Label l2=new Label();
			mw.visitLabel(l2);
			mw.visitLocalVariable("userId", "Ljava/lang/String;", null, l1,l2, 1);
			mw.visitLocalVariable("password", "Ljava/lang/String;", null, l1,l2, 2);
			mw.visitLocalVariable(entityVar, "L"+enCls+";", null, l1,l2, 3);
			
			mw.visitMaxs(0,0); 
			mw.visitEnd();
		}
		
		//public Employee[] getEmployeeList(user,pass,filter)
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "get"+LightStr.capitalize(entityName)+"List", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/String;)[L"+enCls+";", null,new String[]{"java/lang/Exception"}); 
			mw.visitCode();
			Label l1=new Label();
			mw.visitLabel(l1);
			
			mw.visitVarInsn(ALOAD, 0); //this
			
			mw.visitVarInsn(ALOAD, 0); //this
			mw.visitVarInsn(ALOAD, 1); //user
			mw.visitVarInsn(ALOAD, 2); //pass
			
			mw.visitLdcInsn(Type.getType("L"+entityCls+";")); //entity type
			mw.visitLdcInsn(Type.getType("L"+enCls+";")); //wrapper type
			mw.visitVarInsn(ALOAD, 3); //filter
			mw.visitVarInsn(ALOAD, 4); //from
			mw.visitVarInsn(ALOAD, 5); //for
			mw.visitVarInsn(ALOAD, 6); //Sort
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, base, "getEntityList", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/String;)[Ljava/lang/Object;");
			
			mw.visitInsn(DUP);
			mw.visitVarInsn(ASTORE, 7); //array
			mw.visitInsn(ARRAYLENGTH);
			
			mw.visitTypeInsn(ANEWARRAY, enCls);
			mw.visitVarInsn(ASTORE, 8); //empty array
			
			//call copy
			mw.visitVarInsn(ALOAD, 0); //this
			mw.visitVarInsn(ALOAD, 7); //src
			mw.visitVarInsn(ALOAD, 8); //tar
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, base, "arrayCopy", "([Ljava/lang/Object;[Ljava/lang/Object;)V");
			
			mw.visitVarInsn(ALOAD,8);
			
			mw.visitInsn(Opcodes.ARETURN);
			Label l2=new Label();
			mw.visitLabel(l2);
			mw.visitLocalVariable("userId", "Ljava/lang/String;", null, l1,l2, 1);
			mw.visitLocalVariable("password", "Ljava/lang/String;", null, l1,l2, 2);
			mw.visitLocalVariable("jsonFilter", "Ljava/lang/String;", null, l1,l2, 3);
			mw.visitLocalVariable("startRecordNo", "Ljava/lang/Integer;", null, l1,l2, 4);
			mw.visitLocalVariable("maxRecordCount", "Ljava/lang/Integer;", null, l1,l2, 5);
			mw.visitLocalVariable("sortField", "Ljava/lang/String;", null, l1,l2, 6);
			mw.visitMaxs(0, 0); 
			mw.visitEnd();
		}
		
		//public Employee getEmployee(user,pass,filter)
		{
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "get"+LightStr.capitalize(entityName), "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)L"+enCls+";", null,new String[]{"java/lang/Exception"}); 
			mw.visitCode();
			Label l1=new Label();
			mw.visitLabel(l1);
			
			mw.visitVarInsn(ALOAD, 0); //this
			
			mw.visitVarInsn(ALOAD, 0); //this
			mw.visitVarInsn(ALOAD, 1); //user
			mw.visitVarInsn(ALOAD, 2); //pass
			
			mw.visitLdcInsn(Type.getType("L"+entityCls.replace('.', '/')+";")); //entity type
			mw.visitLdcInsn(Type.getType("L"+enCls+";")); //wrapper type
			mw.visitVarInsn(ALOAD, 3); //filter
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, base, "getEntity", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;");
			
			mw.visitTypeInsn(CHECKCAST, enCls); //cast result
			mw.visitInsn(Opcodes.ARETURN); 
			
			Label l2=new Label();
			mw.visitLabel(l2);
			
			mw.visitLocalVariable("userId", "Ljava/lang/String;", null, l1,l2, 1);
			mw.visitLocalVariable("password", "Ljava/lang/String;", null, l1,l2, 2);
			mw.visitLocalVariable("jsonFilter", "Ljava/lang/String;", null, l1,l2, 3);
			
			mw.visitMaxs(0, 0); 
			mw.visitEnd();
		}
	}
	
}
