/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.function.Supplier;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * The common supertype of all AST nodes in a parsed Spring Expression Language
 * format expression.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class SpelNodeImpl implements SpelNode, Opcodes {

	private static final SpelNodeImpl[] NO_CHILDREN = new SpelNodeImpl[0];


	private final int startPos;

	private final int endPos;

	protected SpelNodeImpl[] children = SpelNodeImpl.NO_CHILDREN;

	@Nullable
	private SpelNodeImpl parent;

	/**
	 * Indicates the type descriptor for the result of this expression node.
	 * This is set as soon as it is known. For a literal node it is known immediately.
	 * For a property access or method invocation it is known after one evaluation of
	 * that node.
	 * <p>The descriptor is like the bytecode form but is slightly easier to work with.
	 * It does not include the trailing semicolon (for non array reference types).
	 * Some examples: Ljava/lang/String, I, [I
	 */
	@Nullable
	protected volatile String exitTypeDescriptor;


	public SpelNodeImpl(int startPos, int endPos, @Nullable SpelNodeImpl... operands) {
		this.startPos = startPos;
		this.endPos = endPos;
		if (!ObjectUtils.isEmpty(operands)) {
			this.children = operands;
			for (SpelNodeImpl operand : operands) {
				Assert.notNull(operand, "Operand must not be null");
				operand.parent = this;
			}
		}
	}


	/**
	 * Return {@code true} if the next child is one of the specified classes.
	 */
	protected boolean nextChildIs(Class<?>... classes) {
		if (this.parent != null) {
			SpelNodeImpl[] peers = this.parent.children;
			for (int i = 0, max = peers.length; i < max; i++) {
				if (this == peers[i]) {
					if (i + 1 >= max) {
						return false;
					}
					Class<?> peerClass = peers[i + 1].getClass();
					for (Class<?> desiredClass : classes) {
						if (peerClass == desiredClass) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	@Override
	@Nullable
	public final Object getValue(ExpressionState expressionState) throws EvaluationException {
		return getValueInternal(expressionState).getValue();
	}

	@Override
	public final TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException {
		return getValueInternal(expressionState);
	}

	// by default Ast nodes are not writable
	@Override
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException {
		setValueInternal(expressionState, () -> new TypedValue(newValue));
	}

	/**
	 * Evaluate the expression to a node and then set the new value created by the
	 * specified {@link Supplier} on that node.
	 * <p>For example, if the expression evaluates to a property reference, then the
	 * property will be set to the new value.
	 * <p>Favor this method over {@link #setValue(ExpressionState, Object)} when
	 * the value should be lazily computed.
	 * <p>By default, this method throws a {@link SpelEvaluationException},
	 * effectively disabling this feature. Subclasses may override this method to
	 * provide an actual implementation.
	 * @param expressionState the current expression state (includes the context)
	 * @param valueSupplier a supplier of the new value
	 * @throws EvaluationException if any problem occurs evaluating the expression or
	 * setting the new value
	 * @since 5.2.24
	 */
	public TypedValue setValueInternal(ExpressionState expressionState, Supplier<TypedValue> valueSupplier)
			throws EvaluationException {

		throw new SpelEvaluationException(getStartPosition(), SpelMessage.SETVALUE_NOT_SUPPORTED, getClass().getName());
	}

	@Override
	public SpelNode getChild(int index) {
		return this.children[index];
	}

	@Override
	public int getChildCount() {
		return this.children.length;
	}

	@Override
	@Nullable
	public Class<?> getObjectClass(@Nullable Object obj) {
		if (obj == null) {
			return null;
		}
		return (obj instanceof Class<?> clazz ? clazz : obj.getClass());
	}

	@Override
	public int getStartPosition() {
		return this.startPos;
	}

	@Override
	public int getEndPosition() {
		return this.endPos;
	}

	/**
	 * Determine if this node is the target of a null-safe navigation operation.
	 * <p>The default implementation returns {@code false}.
	 * @return {@code true} if this node is the target of a null-safe operation
	 * @since 6.1.6
	 */
	public boolean isNullSafe() {
		return false;
	}

	@Nullable
	public String getExitDescriptor() {
		return this.exitTypeDescriptor;
	}

	@Nullable
	protected final <T> T getValue(ExpressionState state, Class<T> desiredReturnType) throws EvaluationException {
		return ExpressionUtils.convertTypedValue(state.getEvaluationContext(), getValueInternal(state), desiredReturnType);
	}

	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		throw new SpelEvaluationException(getStartPosition(), SpelMessage.NOT_ASSIGNABLE, toStringAST());
	}

	public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;


	/**
	 * Generate code that handles building the argument values for the specified method.
	 * <p>This method will take into account whether the invoked method is a varargs method,
	 * and if it is then the argument values will be appropriately packaged into an array.
	 * @param mv the method visitor where code should be generated
	 * @param cf the current codeflow
	 * @param member the method or constructor for which arguments are being set up
	 * @param arguments the expression nodes for the expression supplied argument values
	 * @deprecated As of Spring Framework 6.2, in favor of
	 * {@link #generateCodeForArguments(MethodVisitor, CodeFlow, Executable, SpelNodeImpl[])}
	 */
	@Deprecated(since = "6.2")
	protected static void generateCodeForArguments(MethodVisitor mv, CodeFlow cf, Member member, SpelNodeImpl[] arguments) {
		if (member instanceof Executable executable) {
			generateCodeForArguments(mv, cf, executable, arguments);
		}
		throw new IllegalArgumentException(
				"The supplied member must be an instance of java.lang.reflect.Executable: " + member);
	}

	/**
	 * Generate code that handles building the argument values for the specified
	 * {@link Executable} (method or constructor).
	 * <p>This method takes into account whether the invoked executable was
	 * declared to accept varargs, and if it was then the argument values will be
	 * appropriately packaged into an array.
	 * @param mv the method visitor where code should be generated
	 * @param cf the current {@link CodeFlow}
	 * @param executable the {@link Executable} (method or constructor) for which
	 * arguments are being set up
	 * @param arguments the expression nodes for the expression supplied argument
	 * values
	 * @since 6.2
	 */
	protected static void generateCodeForArguments(MethodVisitor mv, CodeFlow cf, Executable executable, SpelNodeImpl[] arguments) {
		Class<?>[] parameterTypes = executable.getParameterTypes();
		String[] paramDescriptors = CodeFlow.toDescriptors(parameterTypes);
		int paramCount = parameterTypes.length;

		if (executable.isVarArgs()) {
			// The final parameter may or may not need packaging into an array, or nothing may
			// have been passed to satisfy the varargs and so something needs to be built.
			int p = 0; // Current supplied argument being processed
			int childCount = arguments.length;

			// Fulfill all the parameter requirements except the last one
			for (p = 0; p < paramCount - 1; p++) {
				cf.generateCodeForArgument(mv, arguments[p], paramDescriptors[p]);
			}

			SpelNodeImpl lastChild = (childCount == 0 ? null : arguments[childCount - 1]);
			ClassLoader classLoader = executable.getDeclaringClass().getClassLoader();
			Class<?> lastChildType = (lastChild != null ?
					loadClassForExitDescriptor(lastChild.getExitDescriptor(), classLoader) : null);
			Class<?> lastParameterType = parameterTypes[paramCount - 1];

			// Determine if the final passed argument is already suitably packaged in array
			// form to be passed to the method
			if (lastChild != null && lastChildType != null && lastParameterType.isAssignableFrom(lastChildType)) {
				cf.generateCodeForArgument(mv, lastChild, paramDescriptors[p]);
			}
			else {
				String arrayType = paramDescriptors[paramCount - 1].substring(1); // trim the leading '[', may leave other '['
				// build array big enough to hold remaining arguments
				CodeFlow.insertNewArrayCode(mv, childCount - p, arrayType);
				// Package up the remaining arguments into the array
				int arrayindex = 0;
				while (p < childCount) {
					SpelNodeImpl child = arguments[p];
					mv.visitInsn(DUP);
					CodeFlow.insertOptimalLoad(mv, arrayindex++);
					cf.generateCodeForArgument(mv, child, arrayType);
					CodeFlow.insertArrayStore(mv, arrayType);
					p++;
				}
			}
		}
		else {
			for (int i = 0; i < paramCount; i++) {
				cf.generateCodeForArgument(mv, arguments[i], paramDescriptors[i]);
			}
		}
	}

	@Nullable
	private static Class<?> loadClassForExitDescriptor(@Nullable String exitDescriptor, ClassLoader classLoader) {
		if (!StringUtils.hasText(exitDescriptor)) {
			return null;
		}

		String typeDescriptor = exitDescriptor;
		if (typeDescriptor.startsWith("[") || typeDescriptor.startsWith("L")) {
			typeDescriptor += ";";
		}
		String className = Type.getType(typeDescriptor).getClassName();
		return ClassUtils.resolveClassName(className, classLoader);
	}

	/**
	 * Ask an argument to generate its bytecode and then follow it up
	 * with any boxing/unboxing/checkcasting to ensure it matches the expected parameter descriptor.
	 * @deprecated As of Spring Framework 6.2, in favor of
	 * {@link CodeFlow#generateCodeForArgument(MethodVisitor, SpelNode, String)}
	 */
	@Deprecated(since = "6.2")
	protected static void generateCodeForArgument(MethodVisitor mv, CodeFlow cf, SpelNodeImpl argument, String paramDesc) {
		cf.generateCodeForArgument(mv, argument, paramDesc);
	}

}
