/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Schorn - initial API and implementation
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp.semantics;

import static org.eclipse.cdt.core.dom.ast.IASTExpression.ValueCategory.LVALUE;
import static org.eclipse.cdt.core.dom.ast.IASTExpression.ValueCategory.PRVALUE;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_alignOf;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_amper;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_minus;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_noexcept;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_not;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_plus;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_postFixDecr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_postFixIncr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_prefixDecr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_prefixIncr;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_sizeof;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_sizeofParameterPack;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_star;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_throw;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_tilde;
import static org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_typeid;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.ExpressionTypes.glvalueType;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.ExpressionTypes.prvalueType;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.ExpressionTypes.prvalueTypeWithResolvedTypedefs;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.ExpressionTypes.valueCategoryFromFunctionCall;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.SemanticUtil.CVTYPE;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.SemanticUtil.REF;
import static org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.SemanticUtil.TDEF;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTExpression.ValueCategory;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IArrayType;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.ISemanticProblem;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IValue;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunction;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunctionType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMember;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateParameterMap;
import org.eclipse.cdt.internal.core.dom.parser.DependentValue;
import org.eclipse.cdt.internal.core.dom.parser.ISerializableEvaluation;
import org.eclipse.cdt.internal.core.dom.parser.ITypeMarshalBuffer;
import org.eclipse.cdt.internal.core.dom.parser.IntegralValue;
import org.eclipse.cdt.internal.core.dom.parser.ProblemType;
import org.eclipse.cdt.internal.core.dom.parser.SizeofCalculator;
import org.eclipse.cdt.internal.core.dom.parser.SizeofCalculator.SizeAndAlignment;
import org.eclipse.cdt.internal.core.dom.parser.ValueFactory;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPArithmeticConversion;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPBasicType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPClosureType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPFunction;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPImplicitFunction;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPPointerToMemberType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPPointerType;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPEvaluation;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPUnknownBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.InstantiationContext;
import org.eclipse.cdt.internal.core.dom.parser.cpp.OverloadableOperator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPSemantics.LookupMode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.EvalUtil.Pair;
import org.eclipse.core.runtime.CoreException;

public class EvalUnary extends CPPDependentEvaluation {
	private static final ICPPEvaluation ZERO_EVAL = new EvalFixed(CPPSemantics.INT_TYPE, PRVALUE, IntegralValue.create(0));

	private final int fOperator;
	private final ICPPEvaluation fArgument;
	private final IBinding fAddressOfQualifiedNameBinding;
	private ICPPFunction fOverload= CPPFunction.UNINITIALIZED_FUNCTION;
	private IType fType;
	private boolean fCheckedIsConstantExpression;
	private boolean fIsConstantExpression;


	public EvalUnary(int operator, ICPPEvaluation operand, IBinding addressOfQualifiedNameBinding,
			IASTNode pointOfDefinition) {
		this(operator, operand, addressOfQualifiedNameBinding, findEnclosingTemplate(pointOfDefinition));
	}

	public EvalUnary(int operator, ICPPEvaluation operand, IBinding addressOfQualifiedNameBinding,
			IBinding templateDefinition) {
		super(templateDefinition);
		fOperator= operator;
		fArgument= operand;
		fAddressOfQualifiedNameBinding= addressOfQualifiedNameBinding;
	}

	public int getOperator() {
		return fOperator;
	}

	public ICPPEvaluation getArgument() {
		return fArgument;
	}

	public IBinding getAddressOfQualifiedNameBinding() {
		return fAddressOfQualifiedNameBinding;
	}

	@Override
	public boolean isInitializerList() {
		return false;
	}

	@Override
	public boolean isFunctionSet() {
		return false;
	}

	@Override
	public boolean isTypeDependent() {
		if (fType != null)
			return fType instanceof TypeOfDependentExpression;

		switch (fOperator) {
		case op_alignOf:
		case op_not:
		case op_sizeof:
		case op_sizeofParameterPack:
		case op_throw:
		case op_typeid:
			return false;
		default:
			return fArgument.isTypeDependent();
		}
	}

	@Override
	public boolean isValueDependent() {
		switch (fOperator) {
		case op_alignOf:
		case op_sizeof:
		case op_sizeofParameterPack:
		case op_typeid:
			return fArgument.isTypeDependent();
		case op_noexcept:
			return fArgument.referencesTemplateParameter();
		case op_throw:
			return false;
		default:
			return fArgument.isValueDependent();
		}
	}

	@Override
	public boolean isConstantExpression(IASTNode point) {
		if (!fCheckedIsConstantExpression) {
			fCheckedIsConstantExpression = true;
			fIsConstantExpression = computeIsConstantExpression(point);
		}
		return fIsConstantExpression;
	}

	private boolean computeIsConstantExpression(IASTNode point) {
		return fArgument.isConstantExpression(point)
			&& isNullOrConstexprFunc(getOverload(point));
	}

	public ICPPFunction getOverload(IASTNode point) {
		if (fOverload == CPPFunction.UNINITIALIZED_FUNCTION) {
			fOverload= computeOverload(point);
		}
		return fOverload;
	}

	private ICPPFunction computeOverload(IASTNode point) {
    	OverloadableOperator op = OverloadableOperator.fromUnaryExpression(fOperator);
		if (op == null)
			return null;

		if (fArgument.isTypeDependent())
			return null;

		if (fAddressOfQualifiedNameBinding instanceof ICPPMember) {
			ICPPMember member= (ICPPMember) fAddressOfQualifiedNameBinding;
			if (!member.isStatic())
				return null;
		}

    	IType type = fArgument.getType(point);
		type = SemanticUtil.getNestedType(type, TDEF | REF | CVTYPE);
		if (!CPPSemantics.isUserDefined(type))
			return null;

		ICPPEvaluation[] args;
	    if (fOperator == op_postFixDecr || fOperator == op_postFixIncr) {
	    	args = new ICPPEvaluation[] { fArgument, ZERO_EVAL };
	    } else {
	    	args = new ICPPEvaluation[] { fArgument };
	    }
    	return CPPSemantics.findOverloadedOperator(point, getTemplateDefinitionScope(), args, type,
    			op, LookupMode.LIMITED_GLOBALS);
	}

	@Override
	public IType getType(IASTNode point) {
		if (fType == null)
			fType= computeType(point);
		return fType;
	}

	private IType computeType(IASTNode point) {
		if (isTypeDependent())
			return new TypeOfDependentExpression(this);

		ICPPFunction overload = getOverload(point);
		if (overload != null)
			return ExpressionTypes.typeFromFunctionCall(overload);

    	switch (fOperator) {
		case op_sizeof:
		case op_sizeofParameterPack:
			return CPPVisitor.get_SIZE_T(point);
		case op_typeid:
			return CPPVisitor.get_type_info(point);
		case op_throw:
			return CPPSemantics.VOID_TYPE;
		case op_amper:
			if (fAddressOfQualifiedNameBinding instanceof ICPPMember) {
				ICPPMember member= (ICPPMember) fAddressOfQualifiedNameBinding;
				if (!member.isStatic()) {
					try {
						return new CPPPointerToMemberType(member.getType(), member.getClassOwner(),
								false, false, false);
					} catch (DOMException e) {
						return e.getProblem();
					}
				}
			}
			return new CPPPointerType(fArgument.getType(point));
		case op_star:
			IType type= fArgument.getType(point);
			type = prvalueTypeWithResolvedTypedefs(type);
	    	if (type instanceof IPointerType) {
	    		return glvalueType(((IPointerType) type).getType());
			}
	    	if (type instanceof ISemanticProblem) {
	    		return type;
	    	}
			return ProblemType.UNKNOWN_FOR_EXPRESSION;
		case op_noexcept:
		case op_not:
			return CPPBasicType.BOOLEAN;
		case op_postFixDecr:
		case op_postFixIncr:
			return prvalueType(fArgument.getType(point));
		case op_plus:
			return promoteType(fArgument.getType(point), true);
		case op_minus:
		case op_tilde:
			return promoteType(fArgument.getType(point), false);
		}
		return fArgument.getType(point);
	}

	private IType promoteType(IType type, boolean allowPointer) {
    	final IType t1 = prvalueType(type);
		final IType t2 = SemanticUtil.getNestedType(t1, TDEF);
		if (allowPointer) {
			if (t2 instanceof CPPClosureType) {
				ICPPMethod conversionOperator = ((CPPClosureType) t2).getConversionOperator();
				if (conversionOperator == null)
					return ProblemType.UNKNOWN_FOR_EXPRESSION;
				return new CPPPointerType(conversionOperator.getType().getReturnType());
			}
			if (t2 instanceof IPointerType) {
				return t1;
			}
		}
		final IType t3= CPPArithmeticConversion.promoteCppType(t2);
		if (t3 == null && !(t2 instanceof IBasicType))
			return ProblemType.UNKNOWN_FOR_EXPRESSION;
		return (t3 == null || t3 == t2) ? t1 : t3;
	}

	@Override
	public IValue getValue(IASTNode point) {
		if (isValueDependent())
			return DependentValue.create(this);

		ICPPEvaluation arg = fArgument;
		ICPPFunction overload = getOverload(point);
		if (overload != null) {
			ICPPFunctionType functionType = overload.getType();
			IType[] parameterTypes = functionType.getParameterTypes();
			if (parameterTypes.length == 0)
				return IntegralValue.ERROR;
			IType targetType = parameterTypes[0];
			arg = maybeApplyConversion(arg, targetType, point);

			if (!(overload instanceof CPPImplicitFunction)) {
				if (!overload.isConstexpr())
					return IntegralValue.ERROR;
				ICPPEvaluation eval = new EvalBinding(overload, null, (IBinding) null);
				arg = new EvalFunctionCall(new ICPPEvaluation[] {eval, arg}, null, (IBinding) null);
				return arg.getValue(point);
			}
		}

		switch (fOperator) {
			case op_sizeof: {
				SizeAndAlignment info =
						SizeofCalculator.getSizeAndAlignment(fArgument.getType(point), point);
				return info == null ? IntegralValue.UNKNOWN : IntegralValue.create(info.size);
			}
			case op_alignOf: {
				SizeAndAlignment info =
						SizeofCalculator.getSizeAndAlignment(fArgument.getType(point), point);
				return info == null ? IntegralValue.UNKNOWN : IntegralValue.create(info.alignment);
			}
			case op_noexcept:
				return IntegralValue.UNKNOWN;  // TODO(sprigogin): Implement
			case op_sizeofParameterPack:
				IValue opVal = fArgument.getValue(point);
				return IntegralValue.create(opVal.numberOfSubValues());
			case op_typeid:
				return IntegralValue.UNKNOWN;  // TODO(sprigogin): Implement
			case op_throw:
				return IntegralValue.UNKNOWN;  // TODO(sprigogin): Implement
		}

		IValue val = arg.getValue(point);
		if (val == null)
			return IntegralValue.UNKNOWN;

		return ValueFactory.evaluateUnaryExpression(fOperator, val);
	}

	@Override
	public ValueCategory getValueCategory(IASTNode point) {
		ICPPFunction overload = getOverload(point);
    	if (overload != null)
    		return valueCategoryFromFunctionCall(overload);

    	switch (fOperator) {
		case op_typeid:
    	case op_star:
    	case op_prefixDecr:
    	case op_prefixIncr:
			return LVALUE;
    	default:
			return PRVALUE;
    	}
    }

	@Override
	public void marshal(ITypeMarshalBuffer buffer, boolean includeValue) throws CoreException {
		buffer.putShort(ITypeMarshalBuffer.EVAL_UNARY);
		buffer.putByte((byte) fOperator);
		buffer.marshalEvaluation(fArgument, includeValue);
		buffer.marshalBinding(fAddressOfQualifiedNameBinding);
		marshalTemplateDefinition(buffer);
	}

	public static ISerializableEvaluation unmarshal(short firstBytes, ITypeMarshalBuffer buffer)
			throws CoreException {
		int op= buffer.getByte();
		ICPPEvaluation arg= (ICPPEvaluation) buffer.unmarshalEvaluation();
		IBinding binding= buffer.unmarshalBinding();
		IBinding templateDefinition= buffer.unmarshalBinding();
		return new EvalUnary(op, arg, binding, templateDefinition);
	}

	@Override
	public ICPPEvaluation instantiate(InstantiationContext context, int maxDepth) {
		ICPPEvaluation argument = fArgument.instantiate(context, maxDepth);
		IBinding binding = fAddressOfQualifiedNameBinding;
		if (binding instanceof ICPPUnknownBinding) {
			try {
				binding= CPPTemplates.resolveUnknown((ICPPUnknownBinding) binding, context);
			} catch (DOMException e) {
			}
		}
		if (argument == fArgument && binding == fAddressOfQualifiedNameBinding)
			return this;

		return new EvalUnary(fOperator, argument, binding, getTemplateDefinition());
	}


	private ICPPEvaluation createOperatorOverloadEvaluation(ICPPFunction overload, IASTNode point, ICPPEvaluation arg) {
		if (overload instanceof ICPPMethod) {
			EvalMemberAccess opAccess = new EvalMemberAccess(arg.getType(point), ValueCategory.LVALUE, overload, arg, false, point);
			ICPPEvaluation[] args = new ICPPEvaluation[]{opAccess};
			return new EvalFunctionCall(args, arg, point);
		} else {
			EvalBinding op = new EvalBinding(overload, overload.getType(), point);
			ICPPEvaluation[] args = new ICPPEvaluation[]{op, arg};
			return new EvalFunctionCall(args, null, point);
		}
	}

	private static boolean isModifyingOperation(int op) {
		return op == op_prefixIncr || op == op_prefixDecr || op == op_postFixIncr || op == op_postFixDecr;
	}

	@Override
	public ICPPEvaluation computeForFunctionCall(ActivationRecord record, ConstexprEvaluationContext context) {
		ICPPFunction overload = getOverload(context.getPoint());
		if (overload != null) {
			ICPPEvaluation operatorCall = createOperatorOverloadEvaluation(overload, context.getPoint(), fArgument);
			ICPPEvaluation eval = operatorCall.computeForFunctionCall(record, context);
			return eval;
		}

		Pair<ICPPEvaluation, ICPPEvaluation> vp = EvalUtil.getValuePair(fArgument, record, context);
		final ICPPEvaluation updateable = vp.getFirst();
		final ICPPEvaluation fixed = vp.getSecond();

		ICPPEvaluation evalUnary = fixed == fArgument || fixed == EvalFixed.INCOMPLETE ? this : new EvalUnary(fOperator, fixed, fAddressOfQualifiedNameBinding, getTemplateDefinition());
		if (fOperator == op_star) {
			if (fixed instanceof EvalPointer) {
				EvalPointer evalPointer = (EvalPointer) fixed;
				return evalPointer.dereference();
			} else if (updateable instanceof EvalBinding && isStarOperatorOnArrayName(context)) {
				EvalBinding evalBinding = (EvalBinding) updateable;
				IBinding binding = evalBinding.getBinding();
				ICPPEvaluation value = record.getVariable(binding);
				EvalCompositeAccess compositeAccess = new EvalCompositeAccess(value, 0);
				return new EvalReference(record, compositeAccess, getTemplateDefinition());
			}
			return evalUnary;
		} else if (fOperator == op_amper) {
			if (updateable instanceof EvalBinding) {
				EvalBinding evalBinding = (EvalBinding) updateable;
				IBinding binding = evalBinding.getBinding();
				return new EvalPointer(record, binding, getTemplateDefinition());
			} else if (updateable instanceof EvalReference) {
				EvalReference evalRef = (EvalReference) updateable;
				return EvalPointer.createFromAddress(evalRef);
			}
			return evalUnary;
		} else if (isModifyingOperation(fOperator)) {
			if (fixed instanceof EvalPointer) {
				EvalPointer evalPointer = (EvalPointer) fixed;
				applyPointerArithmetics(evalPointer);
				return evalPointer;
			} else {
				EvalFixed newValue = new EvalFixed(evalUnary.getType(context.getPoint()), evalUnary.getValueCategory(context.getPoint()), evalUnary.getValue(context.getPoint()));
				if (updateable instanceof EvalReference) {
					EvalReference evalRef = (EvalReference) updateable;
					evalRef.update(newValue);
				} else if (updateable instanceof EvalCompositeAccess) {
					EvalCompositeAccess evalCompAccess = (EvalCompositeAccess) updateable;
					evalCompAccess.update(newValue);
				} else if (updateable instanceof EvalBinding) {
					EvalBinding evalBinding = (EvalBinding) updateable;
					IBinding binding = evalBinding.getBinding();
					record.update(binding, newValue);
				}

				if (this.getValueCategory(context.getPoint()) == ValueCategory.LVALUE) {
					return updateable;
				} else {
					return fixed;
				}
			}
		} else {
			return evalUnary;
		}
	}

	private boolean isStarOperatorOnArrayName(ConstexprEvaluationContext context) {
		return fOperator==op_star && fArgument.getType(context.getPoint()) instanceof IArrayType;
	}

	private void applyPointerArithmetics(EvalPointer poiner) {
		switch (fOperator) {
		case op_postFixIncr:
		case op_prefixIncr:
			poiner.setPosition(poiner.getPosition() + 1);
			break;
		case op_postFixDecr:
		case op_prefixDecr:
			poiner.setPosition(poiner.getPosition() - 1);
			break;
		}
	}

	@Override
	public int determinePackSize(ICPPTemplateParameterMap tpMap) {
		return fArgument.determinePackSize(tpMap);
	}

	@Override
	public boolean referencesTemplateParameter() {
		return fArgument.referencesTemplateParameter();
	}
}
