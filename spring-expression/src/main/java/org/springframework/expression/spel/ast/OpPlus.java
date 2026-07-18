/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * The plus operator will:
 * <ul>
 * <li>add numbers
 * <li>concatenate strings
 * </ul>
 *
 * <p>It can also be used as a unary operator for numbers.
 *
 * <p>The standard promotions are performed when the operand types vary (double + int = double).
 * For other options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Ivo Smid
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 3.0
 */
public class OpPlus extends Operator {

	/**
	 * Maximum number of characters permitted in a concatenated string.
	 * @since 5.2.24
	 */
	private static final int MAX_CONCATENATED_STRING_LENGTH = 100_000;

	/**
	 * Tracks whether a registered {@link TypeConverter} produced a result for a
	 * {@code String + non-String} (or {@code non-String + String}) concatenation that
	 * differs from the natural {@link String#valueOf(Object)} representation.
	 * <p>When {@code true}, the expression cannot be compiled, because the compiled
	 * bytecode uses {@code StringBuilder.append(T)} which is equivalent to
	 * {@code String.valueOf(T)} and therefore not equivalent to the conversion
	 * performed by the {@code TypeConverter} in interpreted mode.
	 * <p>This flag is a one-way latch: once set to {@code true} it is never reset.
	 * @since 7.1
	 */
	private volatile boolean typeConversionDiffersFromToString;


	public OpPlus(int startPos, int endPos, SpelNodeImpl... operands) {
		super("+", startPos, endPos, operands);
		Assert.notEmpty(operands, "Operands must not be empty");
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();

		if (this.children.length < 2) {  // if only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				state.trackOperation();
				if (operandOne instanceof Double) {
					this.exitTypeDescriptor = "D";
				}
				else if (operandOne instanceof Float) {
					this.exitTypeDescriptor = "F";
				}
				else if (operandOne instanceof Long) {
					this.exitTypeDescriptor = "J";
				}
				else if (operandOne instanceof Integer) {
					this.exitTypeDescriptor = "I";
				}
				return new TypedValue(operandOne);
			}
			return state.operate(Operation.ADD, operandOne, null);
		}

		TypedValue operandOneValue = leftOp.getValueInternal(state);
		Object leftOperand = operandOneValue.getValue();
		TypedValue operandTwoValue = getRightOperand().getValueInternal(state);
		Object rightOperand = operandTwoValue.getValue();

		if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
			state.trackOperation();
			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.add(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.add(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double addition
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String leftString && rightOperand instanceof String rightString) {
			this.exitTypeDescriptor = "Ljava/lang/String";
			checkStringLength(leftString);
			checkStringLength(rightString);
			return concatenate(state, leftString, rightString);
		}

		if (leftOperand instanceof String leftString) {
			checkStringLength(leftString);
			String rightString;
			if (rightOperand == null) {
				rightString = "null";
			}
			else {
				rightString = toStringForConcatenation(operandTwoValue, state, rightOperand);
				checkStringLength(rightString);
			}
			this.exitTypeDescriptor = "Ljava/lang/String";
			return concatenate(state, leftString, rightString);
		}

		if (rightOperand instanceof String rightString) {
			checkStringLength(rightString);
			String leftString;
			if (leftOperand == null) {
				leftString = "null";
			}
			else {
				leftString = toStringForConcatenation(operandOneValue, state, leftOperand);
				checkStringLength(leftString);
			}
			this.exitTypeDescriptor = "Ljava/lang/String";
			return concatenate(state, leftString, rightString);
		}

		return state.operate(Operation.ADD, leftOperand, rightOperand);
	}

	private void checkStringLength(String string) {
		checkStringLength(string.length());
	}

	private void checkStringLength(int stringLength) {
		if (stringLength > MAX_CONCATENATED_STRING_LENGTH) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, MAX_CONCATENATED_STRING_LENGTH);
		}
	}

	private TypedValue concatenate(ExpressionState state, String leftString, String rightString) {
		checkStringLength(leftString.length() + rightString.length());
		state.trackOperation();
		return new TypedValue(leftString + rightString);
	}

	@Override
	public String toStringAST() {
		if (this.children.length < 2) {  // unary plus
			return "+" + getLeftOperand().toStringAST();
		}
		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length < 2) {
			throw new IllegalStateException("No right operand");
		}
		return this.children[1];
	}

	/**
	 * Convert the supplied non-{@code null} operand value to a {@code String} for use
	 * in a {@code String + non-String} (or {@code non-String + String}) concatenation.
	 * <p>If the registered {@link TypeConverter} can convert the operand, the converted
	 * result is used and compared against {@link String#valueOf(Object)} to detect
	 * semantic divergence. If they differ, {@link #typeConversionDiffersFromToString}
	 * is set to {@code true} to prevent compilation of this expression node.
	 * <p>If the {@code TypeConverter} cannot convert the operand,
	 * {@link String#valueOf(Object)} is used directly, avoiding a redundant second
	 * conversion and comparison.
	 */
	private String toStringForConcatenation(TypedValue value, ExpressionState state, Object operand) {
		TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
		TypeDescriptor stringDescriptor = TypeDescriptor.valueOf(String.class);
		if (typeConverter.canConvert(value.getTypeDescriptor(), stringDescriptor)) {
			String converterResult = String.valueOf(
					typeConverter.convertValue(operand, value.getTypeDescriptor(), stringDescriptor));
			if (!this.typeConversionDiffersFromToString && !converterResult.equals(String.valueOf(operand))) {
				this.typeConversionDiffersFromToString = true;
			}
			return converterResult;
		}
		// No converter registered for this type: String.valueOf() is the only path,
		// so no comparison is needed and compilation is always safe.
		return String.valueOf(operand);
	}

	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length > 1) {
			if (!getRightOperand().isCompilable()) {
				return false;
			}
		}
		return (this.exitTypeDescriptor != null && !this.typeConversionDiffersFromToString);
	}

	/**
	 * Walk through a possible tree of nodes that combine strings and append
	 * them all to the same (on stack) StringBuilder.
	 */
	private void walk(MethodVisitor mv, CodeFlow cf, @Nullable SpelNodeImpl operand) {
		if (operand instanceof OpPlus plus) {
			walk(mv, cf, plus.getLeftOperand());
			walk(mv, cf, plus.getRightOperand());
		}
		else if (operand != null) {
			cf.enterCompilationScope();
			operand.generateCode(mv, cf);
			String lastDesc = cf.lastDescriptor();
			cf.exitCompilationScope();
			appendToStringBuilder(mv, lastDesc);
		}
	}

	/**
	 * Emit the appropriate {@code StringBuilder.append(T)} call for the value
	 * whose type is described by the supplied descriptor.
	 * <p>Primitive types use the dedicated {@code append} overloads directly.
	 * Reference types use {@code append(Object)}, which delegates to
	 * {@link String#valueOf(Object)} and therefore handles {@code null} safely
	 * (producing {@code "null"}).
	 */
	private static void appendToStringBuilder(MethodVisitor mv, @Nullable String descriptor) {
		if ("Ljava/lang/String".equals(descriptor)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
					"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		}
		else if (CodeFlow.isPrimitive(descriptor)) {
			// byte (B) and short (S) are widened to int on the JVM operand stack
			String appendTypeDesc = switch (descriptor.charAt(0)) {
				case 'B', 'S', 'I' -> "I";
				case 'J' -> "J";
				case 'F' -> "F";
				case 'D' -> "D";
				case 'Z' -> "Z";
				case 'C' -> "C";
				default -> throw new IllegalStateException(
						"Unexpected primitive descriptor '" + descriptor + "'");
			};
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
					"(" + appendTypeDesc + ")Ljava/lang/StringBuilder;", false);
		}
		else {
			// For null and Object reference types, StringBuilder.append(Object) calls
			// String.valueOf(Object), which handles null safely (producing "null") and
			// calls obj.toString() otherwise.
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
					"(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
		}
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if ("Ljava/lang/String".equals(this.exitTypeDescriptor)) {
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
			walk(mv, cf, getLeftOperand());
			walk(mv, cf, getRightOperand());
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		}
		else {
			this.children[0].generateCode(mv, cf);
			String leftDesc = this.children[0].exitTypeDescriptor;
			String exitDesc = this.exitTypeDescriptor;
			Assert.state(exitDesc != null, "No exit type descriptor");
			char targetDesc = exitDesc.charAt(0);
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
			if (this.children.length > 1) {
				cf.enterCompilationScope();
				this.children[1].generateCode(mv, cf);
				String rightDesc = this.children[1].exitTypeDescriptor;
				cf.exitCompilationScope();
				CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
				switch (targetDesc) {
					case 'I' -> mv.visitInsn(IADD);
					case 'J' -> mv.visitInsn(LADD);
					case 'F' -> mv.visitInsn(FADD);
					case 'D' -> mv.visitInsn(DADD);
					default -> throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
				}
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
