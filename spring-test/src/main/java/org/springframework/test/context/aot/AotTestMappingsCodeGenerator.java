/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.aot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedClasses;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.MultiValueMap;

/**
 * Internal code generator for mappings used by {@link AotTestMappings}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class AotTestMappingsCodeGenerator {

	private static final Log logger = LogFactory.getLog(AotTestMappingsCodeGenerator.class);

	private final MultiValueMap<ClassName, Class<?>> classNameMappings;
	private final GeneratedClass generatedClass;


	AotTestMappingsCodeGenerator(MultiValueMap<ClassName, Class<?>> classNameMappings,
			GeneratedClasses generatedClasses) {

		this.classNameMappings = classNameMappings;
		this.generatedClass = generatedClasses.addForFeature("Generated", this::generateType);
	}


	GeneratedClass getGeneratedClass() {
		return this.generatedClass;
	}

	private void generateType(TypeSpec.Builder type) {
		if (logger.isDebugEnabled()) {
			logger.debug("Generating AOT test mappings in " + this.generatedClass.getName().reflectionName());
		}
		type.addJavadoc("Generated mappings for {@link $T}.", AotTestMappings.class);
		type.addModifiers(Modifier.PUBLIC);
		type.addMethod(generateMappingMethod());
	}

	private MethodSpec generateMappingMethod() {
		// Map<String, Supplier<ApplicationContextInitializer<GenericApplicationContext>>>
		ParameterizedTypeName aciType = ParameterizedTypeName.get(
				ClassName.get(ApplicationContextInitializer.class),
				ClassName.get(GenericApplicationContext.class));
		ParameterizedTypeName supplierType = ParameterizedTypeName.get(
				ClassName.get(Supplier.class),
				aciType);
		TypeName mapType = ParameterizedTypeName.get(
				ClassName.get(Map.class),
				ClassName.get(String.class),
				supplierType);

		MethodSpec.Builder method = MethodSpec.methodBuilder("getContextInitializers");
		method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		method.returns(mapType);
		method.addCode(generateMappingCode(mapType));
		return method.build();
	}

	private CodeBlock generateMappingCode(TypeName mapType) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T map = new $T<>()", mapType, HashMap.class);
		this.classNameMappings.forEach((className, testClasses) -> {
			List<String> testClassNames = testClasses.stream().map(Class::getName).toList();
			if (logger.isDebugEnabled()) {
				String contextInitializerName = className.reflectionName();
				logger.debug("Generating mapping from AOT context initializer [%s] to test classes %s"
						.formatted(contextInitializerName, testClassNames));
			}
			testClassNames.forEach(testClassName ->
				code.addStatement("map.put($S, () -> new $T())", testClassName, className));
		});
		code.addStatement("return map");
		return code.build();
	}

}
