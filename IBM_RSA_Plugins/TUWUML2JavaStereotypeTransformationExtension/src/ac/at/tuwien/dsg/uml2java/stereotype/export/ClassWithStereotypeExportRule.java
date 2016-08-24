package ac.at.tuwien.dsg.uml2java.stereotype.export;

/**
 * Class extending the IBM RSA com.ibm.xtools.transform.uml2.java.internal.ClassTransform to also
 * consider the attributes of the applied Stereotype when exporting from UML class diagram to Java code
 * 
 * __author__ = "TU Wien, Distributed System's Group", http://www.infosys.tuwien.ac.at/
 * __copyright__ = "Copyright 2016, TU Wien, Distributed Systems Group"
 * __license__ = "Apache LICENSE V2.0"
 * __maintainer__ = "Daniel Moldovan"
 * __email__ = "d.moldovan@dsg.tuwien.ac.at"
 */

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.jdom.DOMFactory;
import org.eclipse.jdt.core.jdom.IDOMField;
import org.eclipse.jdt.core.jdom.IDOMImport;
import org.eclipse.jdt.core.jdom.IDOMMethod;
import org.eclipse.jdt.core.jdom.IDOMNode;
import org.eclipse.jdt.internal.core.jdom.DOMNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;

import com.ibm.xtools.transform.core.ITransformContext;
import com.ibm.xtools.transform.uml2.impl.internal.java5.ClassRule;


public class ClassWithStereotypeExportRule extends ClassRule {

    private static final List<String> defaultImports;
	
    //TODO: read from a properties file 
	static{
		defaultImports = new ArrayList<String>();
		defaultImports.add("org.eclipse.uml2.uml.*");
		defaultImports.add("org.eclipse.uml2.uml.Package"); // added because some profiles might use it and it might be confused with java.lang.Package
		defaultImports.add("org.eclipse.uml2.uml.Class"); // added because some profiles might use it and it might be confused with java Class type
	}
	
	
	protected Object createTarget(ITransformContext context) {
		  
		
	    /**
	     * Retrieve the target DOM generated by the UML2Java standard transformation
	     */
		DOMNode target = (DOMNode) context.getTarget();
		
		/**
		 * Create a DOMFactory to be used in instantiating new DOM elements
		 */
		DOMFactory domFactory = new DOMFactory();
			
		/**
		 * Get the transformation UML Class
		 */
		Class umlCls = (Class) context.getSource();

		/**
		 * We want to add something as default imports. Mainly org.eclipse.uml2.uml.* but maybe something more in the future.
		 * To do that, as in this rule the Class java file is already partially generated by super ClassRule, we get its parent (i.e., the CompilationUnit), 
		 * remove all parent children (actually just one, the generated class), add all import statements we need, and add the removed children back.
		 * 
		 * Otherwise the imports are added after the end of the class, which triggers compilation errors.
		 */
	    
	    List<IDOMNode> children = new ArrayList<>();
	    IDOMNode parent =  target.getParent();
	    Enumeration enumeration = parent.getChildren();
	    
	    //remove all parent children
	    while(enumeration.hasMoreElements()){
	    	IDOMNode node =	(IDOMNode) enumeration.nextElement();
	    	children.add(node);
	    	node.remove();
	    }
	    
	    //add all import statements
	    for(String importStatement: defaultImports){
		    IDOMImport importEclipseUML = domFactory.createImport();
		    importEclipseUML.setName(importStatement);
		    parent.addChild(importEclipseUML);
	    }
	    
	    //add children back
	    for(IDOMNode child : children){
	    	parent.addChild(child);
	    }
		
		/**
		 * We go through all applied stereotypes and for each we go through each attribute, and create a class field.
		 */
		
		for(Stereotype stereotype : umlCls.getAppliedStereotypes()){
			
			//get all properties
			for (Property attribute :  stereotype.getAllAttributes()) {
				
				String name = attribute.getName();
				Type type = attribute.getType();
				String typeName = type.getName();
			    
				org.eclipse.uml2.uml.Package packageType = type.getPackage();
				/**
				 * Create Java field/class variable for each attribute 
				 */
				Namespace namespace = attribute.getNamespace();
				 
				//create import statement for each added field 
//				IDOMImport importStatement = domFactory.createImport();
//				importStatement.setName(packageType+"."+typeName);
//				
//				target.addChild(importStatement);
				
				IDOMField field = domFactory.createField();
				field.setName(name);
				field.setFlags(Flags.AccPrivate);
				/**
				 * In case the model is incomplete, we add the field with Object as type
				 */
				if (typeName == null){
					field.setType("Object");
					//add in the generated code a comment explaining why field type is Object
					field.setComment("/*Type for attribute \"" + name + "\" on stereotype \"" + stereotype + "\" is null */");
					System.err.println("Type for attribute \"" + name + "\" on stereotype \"" + stereotype + "\" is null");
				}else{
					field.setType(typeName);
				}
			
				target.addChild(field);
				
				/**
				 * Add setter/getter for the added field
				 */
				IDOMMethod setter = domFactory.createMethod();
				/**
				 * Capitalize the first letter of the variable name so we have nice camel-case 
				 */
				setter.setName("set" + name.substring(0, 1).toUpperCase() + name.substring(1));
				setter.setFlags(Flags.AccPublic);
				setter.setReturnType("void");
				setter.addParameter(typeName, name);
				setter.setBody("{ \n this." + name + "=" + name + ";\n }");
				
				target.addChild(setter);
				
				IDOMMethod getter = domFactory.createMethod();
				getter.setName("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
				getter.setFlags(Flags.AccPublic);
				getter.setReturnType(typeName);
				getter.setBody("{ \n return this." + name + ";\n }");
				
				target.addChild(getter);
				
			}
			
		}
		return target;
	}

	public boolean isSourceConsumed(ITransformContext context) {
		return false;
	}

	public boolean canAccept(ITransformContext context) {
		Class umlCls = (Class) context.getSource();
		return umlCls.getStereotypeApplications().size()>0;
	}
	
	
	
}
