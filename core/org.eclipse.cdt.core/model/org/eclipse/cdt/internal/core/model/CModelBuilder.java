/*******************************************************************************
 * Copyright (c) 2001 Rational Software Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     Rational Software - initial implementation
 ******************************************************************************/
package org.eclipse.cdt.internal.core.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.ITemplate;
import org.eclipse.cdt.core.parser.IParser;
import org.eclipse.cdt.core.parser.IQuickParseCallback;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.core.parser.ParserMode;
import org.eclipse.cdt.core.parser.ast.ASTClassKind;
import org.eclipse.cdt.core.parser.ast.ASTNotImplementedException;
import org.eclipse.cdt.core.parser.ast.ASTPointerOperator;
import org.eclipse.cdt.core.parser.ast.IASTAbstractDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTAbstractTypeSpecifierDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTCompilationUnit;
import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTEnumerator;
import org.eclipse.cdt.core.parser.ast.IASTField;
import org.eclipse.cdt.core.parser.ast.IASTFunction;
import org.eclipse.cdt.core.parser.ast.IASTInclusion;
import org.eclipse.cdt.core.parser.ast.IASTMacro;
import org.eclipse.cdt.core.parser.ast.IASTMethod;
import org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition;
import org.eclipse.cdt.core.parser.ast.IASTOffsetableElement;
import org.eclipse.cdt.core.parser.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTSimpleTypeSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTTemplateDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTTemplateParameter;
import org.eclipse.cdt.core.parser.ast.IASTTypeSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTTypedefDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTVariable;
import org.eclipse.cdt.internal.core.parser.ParserException;
import org.eclipse.cdt.internal.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.parser.ast.quick.ASTArrayModifier;
import org.eclipse.core.resources.IProject;


public class CModelBuilder {
	
	protected org.eclipse.cdt.internal.core.model.TranslationUnit translationUnit;
	protected Map newElements;
	protected IQuickParseCallback quickParseCallback; 
	protected IASTCompilationUnit compilationUnit; 
		
	public CModelBuilder(org.eclipse.cdt.internal.core.model.TranslationUnit tu) {
		this.translationUnit = tu ;
		this.newElements = new HashMap();
	}

	protected IASTCompilationUnit parse( String code, boolean hasCppNature, boolean quick, boolean throwExceptionOnError ) throws ParserException
	{
		ParserMode mode = quick ? ParserMode.QUICK_PARSE : ParserMode.COMPLETE_PARSE; 
		quickParseCallback = ParserFactory.createQuickParseCallback(); 
		IParser parser = ParserFactory.createParser( ParserFactory.createScanner( new StringReader( code ), "code", new ScannerInfo(), mode, quickParseCallback), quickParseCallback, mode );
		parser.setCppNature(hasCppNature);
		if( ! parser.parse() && throwExceptionOnError )
			throw new ParserException("Parse failure");
		return quickParseCallback.getCompilationUnit(); 
	}
	
	protected IASTCompilationUnit parse( String code, boolean hasCppNature )throws ParserException
	{
		return parse( code, hasCppNature, true, true );
	}

	public Map parse() throws Exception {

		Map options = null;
		IProject currentProject = null;
		boolean hasCppNature = true;
		
		if (translationUnit != null && translationUnit.getCProject() != null) {
			options = translationUnit.getCProject().getOptions(true);
			currentProject = translationUnit.getCProject().getProject();
		}		
		if( currentProject != null )
		{
			hasCppNature = CoreModel.getDefault().hasCCNature(currentProject);
		}
		
		try
		{
			compilationUnit = parse( translationUnit.getBuffer().getContents(), hasCppNature);		
		}
			
		catch( ParserException e )
		{
			System.out.println( "Parse Exception in Outline View" ); 
			e.printStackTrace();
		}
		long startTime = System.currentTimeMillis();
		try
		{ 
			generateModelElements();
		}
		catch( NullPointerException npe )
		{
			System.out.println( "NullPointer exception generating CModel");
			npe.printStackTrace();
		}
				 
		// For the debuglog to take place, you have to call
		// Util.setDebugging(true);
		// Or set debug to true in the core plugin preference 
		Util.debugLog("CModel build: "+ ( System.currentTimeMillis() - startTime ) + "ms");
		return this.newElements;
		
	}	
	
	protected void generateModelElements(){
		Iterator i = quickParseCallback.iterateOffsetableElements();
		while (i.hasNext()){
			IASTOffsetableElement offsetable = (IASTOffsetableElement)i.next();
			if(offsetable instanceof IASTInclusion){
				createInclusion(translationUnit, (IASTInclusion) offsetable); 		
			}
			else if(offsetable instanceof IASTMacro){
				createMacro(translationUnit, (IASTMacro) offsetable);				
			}else if(offsetable instanceof IASTDeclaration){
				try{
					generateModelElements (translationUnit, (IASTDeclaration) offsetable);
				} catch(ASTNotImplementedException e){
				}
			}
		} 
	}	

	protected void generateModelElements (Parent parent, IASTDeclaration declaration) throws ASTNotImplementedException
	{
		if(declaration instanceof IASTNamespaceDefinition ) {
			generateModelElements(parent, (IASTNamespaceDefinition) declaration);
		}

		if(declaration instanceof IASTAbstractTypeSpecifierDeclaration ) {
			generateModelElements(parent, (IASTAbstractTypeSpecifierDeclaration) declaration);
		}

		if(declaration instanceof IASTTemplateDeclaration ) {
			generateModelElements(parent, (IASTTemplateDeclaration) declaration);
		}

		if(declaration instanceof IASTTypedefDeclaration ) {
			generateModelElements(parent, (IASTTypedefDeclaration) declaration);
		}

/*		if ((declaration instanceof IASTPointerToFunction) 
		|| (declaration instanceof IASTPointerToMethod)) 
		{
			createPointerToFunction(parent, declaration, false); 
		}	
		// variable or field	
		else */ 
		if (declaration instanceof IASTVariable)
		{
			createVariableSpecification(parent, (IASTVariable)declaration, false); 
		}	
		// function or method 
		else if(declaration instanceof IASTFunction ) 
		{
			createFunctionSpecification(parent, (IASTFunction)declaration, false);
		}
	}
	
	protected void generateModelElements (Parent parent, IASTNamespaceDefinition declaration) throws ASTNotImplementedException{
		// IASTNamespaceDefinition 
		IParent namespace = createNamespace(parent, declaration);
		Iterator nsDecls = declaration.getDeclarations();
		while (nsDecls.hasNext()){
			IASTDeclaration subNsDeclaration = (IASTDeclaration) nsDecls.next();
			generateModelElements((Parent)namespace, subNsDeclaration);			
		}
	}
	
	protected void generateModelElements (Parent parent, IASTAbstractTypeSpecifierDeclaration abstractDeclaration) throws ASTNotImplementedException
	{
		// IASTAbstractTypeSpecifierDeclaration 
		 IASTTypeSpecifier typeSpec = abstractDeclaration.getTypeSpecifier(); 
		// IASTEnumerationSpecifier
		if ( typeSpec instanceof IASTEnumerationSpecifier){
			IASTEnumerationSpecifier enumSpecifier = (IASTEnumerationSpecifier) typeSpec;
			IParent enumElement = createEnumeration (parent, enumSpecifier);
		}
		// IASTClassSpecifier
		else if (typeSpec instanceof IASTClassSpecifier){
			IASTClassSpecifier classSpecifier = (IASTClassSpecifier) typeSpec;
			IParent classElement = createClass (parent, classSpecifier, false);
			// create the sub declarations 
			Iterator j = classSpecifier.getDeclarations();
			while (j.hasNext()){
				IASTDeclaration subDeclaration = (IASTDeclaration)j.next();
				generateModelElements((Parent)classElement, subDeclaration);					
			} // end while j
		}
	}

	protected void generateModelElements (Parent parent, IASTTemplateDeclaration templateDeclaration) throws ASTNotImplementedException
	{				
		// Template Declaration 
		IASTDeclaration declaration = (IASTDeclaration)templateDeclaration.getOwnedDeclaration();
		if(declaration instanceof IASTAbstractTypeSpecifierDeclaration){
			IASTAbstractTypeSpecifierDeclaration abstractDeclaration = (IASTAbstractTypeSpecifierDeclaration)declaration ;			
			IASTTypeSpecifier typeSpec = abstractDeclaration.getTypeSpecifier();
			if (typeSpec instanceof IASTClassSpecifier){
				IASTClassSpecifier classSpecifier = (IASTClassSpecifier) typeSpec;
				ITemplate classTemplate = (StructureTemplate)createClass(parent, classSpecifier, true);
				CElement element = (CElement) classTemplate;
				
				// set the element position		
				element.setPos(templateDeclaration.getStartingOffset(), templateDeclaration.getEndingOffset() - templateDeclaration.getStartingOffset());	
				// set the element lines
				//element.setLines(templateDeclaration.getTopLine(), templateDeclaration.getBottomLine());
				// set the template parameters				
				String[] parameterTypes = getTemplateParameters(templateDeclaration);
				classTemplate.setTemplateParameterTypes(parameterTypes);				
	
				// create the sub declarations 
				Iterator j  = classSpecifier.getDeclarations();
				while (j.hasNext()){
					IASTDeclaration subDeclaration = (IASTDeclaration)j.next();
					generateModelElements((Parent)classTemplate, subDeclaration);					
				} // end while j
			}

		}
		ITemplate template = null;

/*		if ((declaration instanceof IASTPointerToFunction) 
			|| (declaration instanceof IASTPointerToMethod))
		{
			template = (ITemplate) createPointerToFunction(parent, declaration, true); 
		}	
		// template of variable or field	
		else */ 
		if (declaration instanceof IASTVariable) 
		{
			template = (ITemplate) createVariableSpecification(parent, (IASTVariable)declaration, true); 
		}	
		// Template of function or method 
		else if(declaration instanceof IASTFunction ) 
		{
			template = (ITemplate) createFunctionSpecification(parent, (IASTFunction)declaration, true);
		}		
	 	 
 		if(template != null){
			CElement element = (CElement)template;
			// set the element position		
			element.setPos(templateDeclaration.getStartingOffset(), templateDeclaration.getEndingOffset() - templateDeclaration.getStartingOffset());	
			// set the element lines
			//element.setLines(templateDeclaration.getTopLine(), templateDeclaration.getBottomLine());
			// set the template parameters
			String[] parameterTypes = getTemplateParameters(templateDeclaration);	
			template.setTemplateParameterTypes(parameterTypes);				
		}
	}

	protected void generateModelElements (Parent parent, IASTTypedefDeclaration declaration) throws ASTNotImplementedException
	{
		TypeDef typeDef = createTypeDef(parent, declaration);
		IASTAbstractDeclaration abstractDeclaration = declaration.getAbstractDeclarator();
		generateModelElements(parent, abstractDeclaration);
	}
	
	protected void generateModelElements (Parent parent, IASTAbstractDeclaration abstractDeclaration) throws ASTNotImplementedException{
		/*-------------------------------------------
		 * Checking the type if it is a composite one
		 *-------------------------------------------*/
		 IASTTypeSpecifier typeSpec = abstractDeclaration.getTypeSpecifier(); 
		// IASTEnumerationSpecifier
		if ( typeSpec instanceof IASTEnumerationSpecifier){
			IASTEnumerationSpecifier enumSpecifier = (IASTEnumerationSpecifier) typeSpec;
			IParent enumElement = createEnumeration (parent, enumSpecifier);
		}
		// IASTClassSpecifier
		else if (typeSpec instanceof IASTClassSpecifier){
			IASTClassSpecifier classSpecifier = (IASTClassSpecifier) typeSpec;
			IParent classElement = createClass (parent, classSpecifier, false);
			// create the sub declarations 
			Iterator j = classSpecifier.getDeclarations();
			while (j.hasNext()){
				IASTDeclaration subDeclaration = (IASTDeclaration)j.next();
				generateModelElements((Parent)classElement, subDeclaration);					
			} // end while j
		}
	}
	
	protected Include createInclusion(Parent parent, IASTInclusion inclusion){
		// create element
		Include element = new Include((CElement)parent, inclusion.getName(), !inclusion.isLocal());
		// add to parent
		parent.addChild((CElement) element);
		// set position
		element.setIdPos(inclusion.getNameOffset(), inclusion.getName().length());
		element.setPos(inclusion.getStartingOffset(), inclusion.getEndingOffset() - inclusion.getStartingOffset());
		// set the element lines
		//element.setLines(inclusion.getTopLine(), inclusion.getBottomLine());
		this.newElements.put(element, element.getElementInfo());
		return element;
	}
	
	protected Macro createMacro(Parent parent, IASTMacro macro){
		// create element
		org.eclipse.cdt.internal.core.model.Macro element = new  Macro(parent, macro.getName());
		// add to parent
		parent.addChild((CElement) element);		
		// set position
		element.setIdPos(macro.getNameOffset(), macro.getName().length());
		element.setPos(macro.getStartingOffset(), macro.getEndingOffset() - macro.getStartingOffset());
		// set the element lines
		//element.setLines(macro.getTopLine(), macro.getBottomLine());
		this.newElements.put(element, element.getElementInfo());
		return element;
	}
	
	protected Namespace createNamespace(Parent parent, IASTNamespaceDefinition nsDef){
		// create element
		String type = "namespace";
		String nsName = (nsDef.getName() == null )  
						? "" 
						: nsDef.getName().toString();
		Namespace element = new Namespace ((ICElement)parent, nsName );
		// add to parent
		parent.addChild((ICElement)element);
		element.setIdPos(nsDef.getNameOffset(), (nsName.length() == 0) ? type.length() : nsName.length());
		element.setPos(nsDef.getStartingOffset(), nsDef.getEndingOffset() - nsDef.getStartingOffset());
		element.setTypeName(type);
		// set the element lines
		//element.setLines(nsDef.getTopLine(), nsDef.getBottomLine());
		
		this.newElements.put(element, element.getElementInfo());		
		return element;
	}

	protected Enumeration createEnumeration(Parent parent, IASTEnumerationSpecifier enumSpecifier){
		// create element
		String type = "enum";
		String enumName = (enumSpecifier.getName() == null )
						  ? "" 
						  : enumSpecifier.getName().toString();
		Enumeration element = new Enumeration ((ICElement)parent, enumName );
		// add to parent
		parent.addChild((ICElement)element);
		Iterator i  = enumSpecifier.getEnumerators();
		while (i.hasNext()){
			// create sub element
			IASTEnumerator enumDef = (IASTEnumerator) i.next();
			createEnumerator(element, enumDef);
		}
		// set enumeration position
		element.setIdPos(enumSpecifier.getNameOffset(), (enumName.length() == 0) ? type.length() : enumName.length());
		element.setPos(enumSpecifier.getStartingOffset(), enumSpecifier.getEndingOffset() - enumSpecifier.getStartingOffset());
		element.setTypeName(type);
		// set the element lines
		//element.setLines(enumSpecifier.getTopLine(), enumSpecifier.getBottomLine());
		 
		this.newElements.put(element, element.getElementInfo());
		return element;
	}
	
	protected Enumerator createEnumerator(Parent enum, IASTEnumerator enumDef){
		Enumerator element = new Enumerator (enum, enumDef.getName().toString());
		// add to parent
		enum.addChild(element);
		// set enumerator position
		element.setIdPos(enumDef.getStartingOffset(), enumDef.getName().length());
		element.setPos(enumDef.getStartingOffset(), enumDef.getEndingOffset() - enumDef.getStartingOffset());
		// set the element lines
		//element.setLines(enumDef.getTopLine(), enumDef.getBottomLine());

		this.newElements.put(element, element.getElementInfo());
		return element;		
	}
	
	protected Structure createClass(Parent parent, IASTClassSpecifier classSpecifier, boolean isTemplate){
		// create element
		String className = "";
		String type = "";
		int kind = ICElement.C_CLASS;
		ASTClassKind classkind = classSpecifier.getClassKind();
		if(classkind == ASTClassKind.CLASS){
			if(!isTemplate)
				kind = ICElement.C_CLASS;
			else
				kind = ICElement.C_TEMPLATE_CLASS;
			type = "class";
			className = (classSpecifier.getName() == null )
						? ""
						: classSpecifier.getName().toString();				
		}
		if(classkind == ASTClassKind.STRUCT){
			if(!isTemplate)
				kind = ICElement.C_STRUCT;
			else
				kind = ICElement.C_TEMPLATE_STRUCT;
			type = "struct";
			className = (classSpecifier.getName() == null ) 
						? "" 
						: classSpecifier.getName().toString();				
		}
		if(classkind == ASTClassKind.UNION){
			if(!isTemplate)
				kind = ICElement.C_UNION;
			else
				kind = ICElement.C_TEMPLATE_UNION;
			type = "union";
			className = (classSpecifier.getName() == null )
						? "" 
						: classSpecifier.getName().toString();				
		}
		
		Structure element;
		if(!isTemplate){		
			Structure classElement = new Structure( (CElement)parent, kind, className );
			element = classElement;
		} else {
			StructureTemplate classTemplate = new StructureTemplate( (CElement)parent, kind, className );
			element = classTemplate;
		}
		

		// add to parent
		parent.addChild((ICElement) element);
		// set element position 
		element.setIdPos( classSpecifier.getNameOffset(), (className.length() == 0) ? type.length() : className.length() );
		element.setTypeName( type );
		if(!isTemplate){
			// set the element position
			element.setPos(classSpecifier.getStartingOffset(), classSpecifier.getEndingOffset() - classSpecifier.getStartingOffset());
			// set the element lines
			//element.setLines(classSpecifier.getTopLine(), classSpecifier.getBottomLine());
		}
		
		this.newElements.put(element, element.getElementInfo());
		return element;
	}
	
	protected TypeDef createTypeDef(Parent parent, IASTTypedefDeclaration typeDefDeclaration){
		// create the element
		String name = typeDefDeclaration.getName();
        
        TypeDef element = new TypeDef( parent, name );
        
        StringBuffer typeName = new StringBuffer(getType(typeDefDeclaration.getAbstractDeclarator()));
		element.setTypeName(typeName.toString());
		
		// add to parent
		parent.addChild((CElement)element);

		// set positions
		element.setIdPos(typeDefDeclaration.getNameOffset(),name.length());	
		element.setPos(typeDefDeclaration.getStartingOffset(), typeDefDeclaration.getEndingOffset() - typeDefDeclaration.getStartingOffset());
		// set the element lines
		//element.setLines(simpleDeclaration.getTopLine(), simpleDeclaration.getBottomLine());

		this.newElements.put(element, element.getElementInfo());
		return element;	
	}

	protected VariableDeclaration createVariableSpecification(Parent parent, IASTVariable varDeclaration, boolean isTemplate)
    {
		String variableName = varDeclaration.getName(); 
		if(variableName == null){
			// something is wrong, skip this element
			return null;
		}
		VariableDeclaration element = null;
		if(varDeclaration instanceof IASTField){
			IASTField fieldDeclaration = (IASTField) varDeclaration;
			// field
			Field newElement = new Field( parent, variableName);
			newElement.setMutable(fieldDeclaration.isMutable());
			newElement.setVisibility(fieldDeclaration.getVisiblity());
			element = newElement;			
		}
		else {
			if(isTemplate){
				// variable
				VariableTemplate newElement = new VariableTemplate( parent, variableName );
				element = newElement;									
			}else {
				if(varDeclaration.isExtern()){
					// variableDeclaration
					VariableDeclaration newElement = new VariableDeclaration( parent, variableName );
					element = newElement;
				}
				else {
					// variable
					Variable newElement = new Variable( parent, variableName );
					element = newElement;				
				}
			}
		}
		element.setTypeName ( getType(varDeclaration.getAbstractDeclaration()) );
		element.setConst(varDeclaration.getAbstractDeclaration().isConst());
		// TODO : fix volatile for variables
		// element.setVolatile(varDeclaration.isVolatile());
		element.setStatic(varDeclaration.isStatic());
		// add to parent
		parent.addChild( element ); 	

		// set position
		element.setIdPos( varDeclaration.getNameOffset(), variableName.length() );
		if(!isTemplate){
			// set element position
			element.setPos(varDeclaration.getStartingOffset(), varDeclaration.getEndingOffset() - varDeclaration.getStartingOffset());
			// set the element lines
			//element.setLines(simpleDeclaration.getTopLine(), simpleDeclaration.getBottomLine());
		}
			
		this.newElements.put(element, element.getElementInfo());
		return element;
	}

	protected FunctionDeclaration createFunctionSpecification(Parent parent, IASTFunction functionDeclaration, boolean isTemplate)
    {    	
		String name = functionDeclaration.getName();
        if (name == null) {
            // Something is wrong, skip this element
            return null;             
        } 

		// get parameters types
		String[] parameterTypes = getFunctionParameterTypes(functionDeclaration);
		
		FunctionDeclaration element = null;
		
		if( functionDeclaration instanceof IASTMethod )
		{
			IASTMethod methodDeclaration = (IASTMethod) functionDeclaration;
			if (methodDeclaration.hasFunctionBody())
			{
				// method
				if(!isTemplate){
					Method newElement = new Method( parent, name );
					newElement.setVisibility(methodDeclaration.getVisiblity());
					element = newElement;				
				}else {
					MethodTemplate newElement = new MethodTemplate(parent, name);
					newElement.setVisibility(methodDeclaration.getVisiblity());
					element = newElement;				
				}
			}
			else
			{
				// method declaration
				if(!isTemplate){
					MethodDeclaration newElement = new MethodDeclaration( parent, name );
					newElement.setVisibility(methodDeclaration.getVisiblity());
					element = newElement;				
				}else {
					MethodTemplate newElement = new MethodTemplate(parent, name);
					newElement.setVisibility(methodDeclaration.getVisiblity());
					element = newElement;				
				}
				
			}
		}
		else // instance of IASTFunction 
		{
			if (functionDeclaration.hasFunctionBody())
			{				
				// function
				if(!isTemplate){
					Function newElement = new Function( parent, name );
					element = newElement;				
				} else {
					FunctionTemplate newElement = new FunctionTemplate( parent, name );
					element = newElement;
				}
			}
			else
			{
				// functionDeclaration
				if(!isTemplate){
					FunctionDeclaration newElement = new FunctionDeclaration( parent, name );
					element = newElement;				
				} else {
					FunctionTemplate newElement = new FunctionTemplate( parent, name );
					element = newElement;
				}
			}
		}						
		element.setParameterTypes(parameterTypes);
		element.setReturnType( getType(functionDeclaration.getReturnType()) );
		// TODO: Fix volatile and const
		//element.setVolatile(functionDeclaration.isVolatile());
		element.setStatic(functionDeclaration.isStatic());
		//element.setConst(functionDeclaration.isConst());				

		// add to parent
		parent.addChild( element ); 	

		// hook up the offsets
		element.setIdPos( functionDeclaration.getNameOffset(), name.length() );
		if(!isTemplate){
			// set the element position		
			element.setPos(functionDeclaration.getStartingOffset(), functionDeclaration.getEndingOffset() - functionDeclaration.getStartingOffset());	
			// set the element lines
			//element.setLines(simpleDeclaration.getTopLine(), simpleDeclaration.getBottomLine());
		}

		this.newElements.put(element, element.getElementInfo());
		return element;
	}

	private String[] getTemplateParameters(Iterator templateParams){
		List paramList = new ArrayList();
		while (templateParams.hasNext()){
			StringBuffer paramType = new StringBuffer();
			IASTTemplateParameter parameter = (IASTTemplateParameter)templateParams.next();
			if((parameter.getIdentifier() != null) && (parameter.getIdentifier().length() != 0))
			{
				paramList.add(parameter.getIdentifier().toString());
			}
			else
			{				
				IASTTemplateParameter.ParamKind kind = parameter.getTemplateParameterKind();
				if(kind == IASTTemplateParameter.ParamKind.CLASS){
					paramType.append("class");
				}
				if(kind == IASTTemplateParameter.ParamKind.TYPENAME){
					paramType.append("typename");
				}
				if(kind == IASTTemplateParameter.ParamKind.TEMPLATE_LIST){
					paramType.append("template<");
					String[] subParams = getTemplateParameters(parameter.getTemplateParameters());
					int p = 0; 
					if ( subParams.length > 0)
						paramType.append(subParams[p++]);
					while( p < subParams.length){
						paramType.append(", ");
						paramType.append(subParams[p++]);							
					}
					paramType.append(">");
				}
				if(kind == IASTTemplateParameter.ParamKind.PARAMETER){
					paramType.append(getType(parameter.getParameterDeclaration()));				
				}
				paramList.add(paramType.toString());
			} // end else
		}// end while
		String[] parameterTypes = new String[paramList.size()];
		for(int j=0; j<paramList.size(); ++j){
			parameterTypes[j] = (String) paramList.get(j);			
		}
		return parameterTypes;		
		
	}	
	private String[] getTemplateParameters(IASTTemplateDeclaration templateDeclaration){
		// add the parameters
		Iterator i = templateDeclaration.getTemplateParameters();
		return getTemplateParameters(i);
	}
		
	private String getType(IASTAbstractDeclaration declaration)
	{
		StringBuffer type = new StringBuffer();
			
		// get type from declaration
		type.append(getDeclarationType(declaration));
		type.append(getSubtype(declaration));
		
//		type.append(getSubType(declarator, new SubTypeProcessingFlags(false)));
		
		return type.toString();
	}
	

	private String getSubtype(IASTAbstractDeclaration declaration){
		StringBuffer type = new StringBuffer();
		type.append(getPointerOperation(declaration));
		type.append(getArrayQualifiers(declaration));
		return type.toString();		
	}
    
/*    private class SubTypeProcessingFlags {
        boolean returnTypeForFunction = false;
        boolean processedInnermostParameterList = false;
        
        SubTypeProcessingFlags(boolean returnTypeForFunction) {
            this.returnTypeForFunction = returnTypeForFunction;
        }
    }
*/	
/*	private String getSubType(Declarator declarator, SubTypeProcessingFlags flags) {
		StringBuffer type = new StringBuffer();
						
		// add pointer or reference from declarator if any
		String declaratorPointerOperation = getDeclaratorPointerOperation(declarator);
		try  {
			switch (declaratorPointerOperation.charAt(0)) {
				case '*':
				case '&':
					break; // pointer/reference
				default:
					type.append(" "); // pointer to member
			}
		} catch (Exception e) {} // Empty/null strings
		type.append(declaratorPointerOperation);
        
        String subType = null;
						
		if (declarator.getDeclarator() != null){
			// process inner declarator
			subType = getSubType(declarator.getDeclarator(), flags);
			boolean appendParen = true;
			
			if (  (subType == null) || (subType.length() == 0)
			    ||
			    	((subType.charAt(0) == '(') 
			      && 
			        (subType.charAt(subType.length()-1) == ')'))) {
			        	
			        		// Additional () are not necessary
			        		appendParen = false;
	        }
			
			if (appendParen) type.append("(");
			type.append(subType);
			if (appendParen) type.append(")");
		}			
			
		// parameters
		if (declarator.getParms() != null) { 
            // If we process return type for a function,
            // skip innermost parameter list - it is a part
            // of function's signature
            if ( !flags.returnTypeForFunction 
               || flags.processedInnermostParameterList) {
                   
                   if ((subType == null) || (subType.length() == 0)) {
                       type.append("()");
                   }

                   type.append(getParametersString(declarator));
            }
            flags.processedInnermostParameterList = true;
		}
				 
		// arrays
		type.append(getDeclaratorArrayQualifiers(declarator));
			
		return type.toString();
	}
*/    
    
    /**
     *  Here is a tricky one. Determines if a declarator represents a function
     * specification, or a variable declaration (that includes pointers to functions).
     * If none of the nested declarators contain parameter list, then it is obviously a variable.
     * It is a function specification only if no declarators in (A..B] range
     * contain any pointer/array specificators. A is the declarator containing 
     * the innermost parameter list (which corresponds to parameters of the function),
     * and B is the innermost declarator (should contain the name of the element).
     * 
     * @param declarator
     * @return True, if the declarator represents a function specification
     */
    
/*    private boolean isFunctionSpecification(Declarator declarator)
    {
        Declarator currentDeclarator = declarator;
        boolean result = false;
        
        while (currentDeclarator != null) {
            if (currentDeclarator.getParms() != null) {
                result = true;
            } else {          
                List ptrOps = currentDeclarator.getPointerOperators();
                List arrayQs = currentDeclarator.getArrayQualifiers();
                
                if (    ((ptrOps != null) && (ptrOps.size() > 0)) 
                     || ((arrayQs != null) && (arrayQs.size() > 0)) 
                   )  
                   {
                    result = false;
                } 
            }
            
            currentDeclarator = currentDeclarator.getDeclarator();
        }
        
        return result;
    }
*/	
	private String getDeclarationType(IASTAbstractDeclaration declaration){
		StringBuffer type = new StringBuffer();
		
		if(declaration.isConst())
			type.append("const ");
		// TODO: Fix volatile
//		if(declaration.isVolatile())
//			type.append("volatile ");
		IASTTypeSpecifier typeSpecifier = declaration.getTypeSpecifier();
		if(typeSpecifier instanceof IASTElaboratedTypeSpecifier){
			IASTElaboratedTypeSpecifier elab = (IASTElaboratedTypeSpecifier) typeSpecifier;
			type.append(getElaboratedTypeSignature(elab));
		}else if(typeSpecifier instanceof IASTSimpleTypeSpecifier){		
			IASTSimpleTypeSpecifier simpleSpecifier = (IASTSimpleTypeSpecifier) typeSpecifier;		
			type.append(simpleSpecifier.getTypename());
		} 
		return type.toString();	
	}
	
	private String getElaboratedTypeSignature(IASTElaboratedTypeSpecifier elab){
		StringBuffer type = new StringBuffer();
		ASTClassKind t = elab.getClassKind();
		if( t == ASTClassKind.CLASS){
			type.append("class");
		} 
		else if( t == ASTClassKind.STRUCT){
			type.append("struct");
		}
		else if( t == ASTClassKind.UNION){
			type.append("union");
		}
		else if( t == ASTClassKind.STRUCT){
			type.append("enum");
		}
		type.append(" ");
		type.append(elab.getName().toString());
		return type.toString();
	}
	
	private String getPointerOperation(IASTAbstractDeclaration declaration){		
		StringBuffer pointerString = new StringBuffer();
		Iterator i = declaration.getPointerOperators();
		while(i.hasNext()){
			ASTPointerOperator po = (ASTPointerOperator) i.next();
			if(po == ASTPointerOperator.POINTER)
				pointerString.append("*");

			if(po == ASTPointerOperator.REFERENCE)
				pointerString.append("&");

			if(po == ASTPointerOperator.CONST_POINTER)
				pointerString.append(" const");

			if(po == ASTPointerOperator.VOLATILE_POINTER)
				pointerString.append(" volatile");
				
//				case PointerOperator.t_pointer_to_member:
//					pointerString.append(po.getNameSpecifier());
		}
		return pointerString.toString();
	}

	private String getArrayQualifiers(IASTAbstractDeclaration declaration){		
		StringBuffer arrayString = new StringBuffer();
		Iterator i  = declaration.getArrayModifiers(); 
		while (i.hasNext()){
			ASTArrayModifier q = (ASTArrayModifier) i.next();
			arrayString.append("[]");				
		}
		return arrayString.toString();
	}
	
    	
/*	private String[] getParameterTypes(Declarator declarator, HashMap mapOfKRParams) 
	{	
		if (declarator == null) return null;
		
		ParameterDeclarationClause pdc = declarator.getParms();
		String[] parameterTypes = null;
		
		if (pdc != null) {
			List parameterList = pdc.getDeclarations();
			parameterTypes = new String[parameterList.size()];

			for (int j = 0; j < parameterList.size(); ++j) {
				ParameterDeclaration param = (ParameterDeclaration) parameterList.get(j);
                Declarator decl = (Declarator) param.getDeclarators().get(0);
				parameterTypes[j] =	getType(param);
                
                if (    (mapOfKRParams != null) 
                    &&  (mapOfKRParams.size() > 0) 
                    && (decl.getName() == null)) 
                {
                    // We have some K&R-style parameter declarations,
                    // and the current parameter has been declared with a single identifier,
                    // (like  ...(argname)...)
                    // It has been parsed as a typename, so 'argname' is a name of the type,
                    // and parameter name is empty. But for this particular case, 
                    // 'argname' is a name, and its type we have to lookup in the map
                    // of old K&R-style parameter declarations.
                    // If we can't find it, we keep parameter name in the signature
                    String oldKRParamType = (String)mapOfKRParams.get(parameterTypes[j]);
                    if (oldKRParamType != null) {
                        parameterTypes[j] = oldKRParamType; 
                    }
                }
			}
		}
		
		return parameterTypes;
	}
*/    
    private String[] getFunctionParameterTypes(IASTFunction functionDeclaration)
    {
    	Iterator parameters = functionDeclaration.getParameters();
    	List paramList = new ArrayList();
    	while (parameters.hasNext()){
			IASTParameterDeclaration param = (IASTParameterDeclaration)parameters.next();
			paramList.add(getType(param));
    	}
		String[] parameterTypes = new String[paramList.size()];
		for(int i=0; i<paramList.size(); ++i){
			parameterTypes[i] = (String)paramList.get(i); 
		}
    	return parameterTypes;
    	
/*        Declarator currentDeclarator = declarator;
        Declarator innermostPDCDeclarator = null;

        while (currentDeclarator != null) {
            if (currentDeclarator.getParms() != null) {
                innermostPDCDeclarator = currentDeclarator;
            }
            currentDeclarator = currentDeclarator.getDeclarator();
        }
        
        HashMap mapOfKRParams = null;
        
        if (    declarator != null 
            && declarator.getParms() != null 
            && declarator.getParms().getOldKRParms() != null) {
                
            mapOfKRParams = new HashMap();
                
            OldKRParameterDeclarationClause oldKRpdc = declarator.getParms().getOldKRParms();
            List oldKRParameterList = oldKRpdc.getDeclarations();
            
            for (int j = 0; j < oldKRParameterList.size(); ++j) {
                if(oldKRParameterList.get(j) instanceof SimpleDeclaration) { // Must be
                    SimpleDeclaration declKR = (SimpleDeclaration)oldKRParameterList.get(j);
                    
                    List declarators = declKR.getDeclarators();
                    Iterator d = declarators.iterator();
                    while (d.hasNext()) {
                        Declarator decl = (Declarator) d.next();
                        
						String oldKRparamName = getDOMName(decl);                        
                        String oldKRparamType = getType(declKR, decl);
    
                        if (   (oldKRparamType != null)
                            && (oldKRparamName != null)
                            && (oldKRparamName.toString().length() > 0)
                            ) {
                                mapOfKRParams.put(oldKRparamName.toString(), oldKRparamType);
                        }
                    }
                }
            }
        }

        return getParameterTypes(innermostPDCDeclarator, mapOfKRParams);
*/	
    }
	
	private String getParametersString(String[] parameterTypes) 
	{
		StringBuffer parameters = new StringBuffer("");
		
		if ((parameterTypes != null) && (parameterTypes.length > 0)) {
			parameters.append("(");
			int i = 0;
			parameters.append(parameterTypes[i++]);
			while (i < parameterTypes.length) {
				parameters.append(", ");
				parameters.append(parameterTypes[i++]);
			}
			parameters.append(")");
		} else {
			if (parameterTypes != null) parameters.append("()");
		}
		
		return parameters.toString();
	}	    
}
