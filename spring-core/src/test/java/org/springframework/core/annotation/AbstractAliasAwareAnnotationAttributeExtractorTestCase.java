/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Abstract base class for tests involving concrete implementations of
 * {@link AbstractAliasAwareAnnotationAttributeExtractor}.
 *
 * @author sbrannen
 * @since 4.2.1
 */
@RunWith(Parameterized.class)
public abstract class AbstractAliasAwareAnnotationAttributeExtractorTestCase {

	@Parameters(name = "{1}")
	public static Object[][] testData() {
		return new Object[][] {
			{ GroovyMultipleAliasesComposedContextConfigClass.class, "groovyScript" },
			{ XmlMultipleAliasesComposedContextConfigClass.class, "xmlFile" },
			{ ValueMultipleAliasesComposedContextConfigClass.class, "value" }
		};
	}


	@Parameter(0)
	public Class<?> clazz;

	@Parameter(1)
	public String expected;


	@Test
	public void getAttributeValueForMultipleImplicitAliases() throws Exception {
		Method xmlFile = MultipleAliasesComposedContextConfig.class.getDeclaredMethod("xmlFile");
		Method groovyScript = MultipleAliasesComposedContextConfig.class.getDeclaredMethod("groovyScript");
		Method value = MultipleAliasesComposedContextConfig.class.getDeclaredMethod("value");

		AnnotationAttributeExtractor<?> extractor = createExtractorFor(MultipleAliasesComposedContextConfig.class);

		assertThat(extractor.getAttributeValue(value), is(expected));
		assertThat(extractor.getAttributeValue(groovyScript), is(expected));
		assertThat(extractor.getAttributeValue(xmlFile), is(expected));
	}

	protected abstract AnnotationAttributeExtractor<?> createExtractorFor(Class<? extends Annotation> annotationType);


	/**
	 * Mock of {@code org.springframework.test.context.ContextConfiguration}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface ContextConfig {

		@AliasFor("location")
		String value() default "";

		@AliasFor("value")
		String location() default "";
	}

	@ContextConfig
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface MultipleAliasesComposedContextConfig {

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String xmlFile() default "";

		@AliasFor(annotation = ContextConfig.class, value = "location")
		String groovyScript() default "";

		@AliasFor(annotation = ContextConfig.class, attribute = "location")
		String value() default "";
	}

	// Attribute value intentionally matches attribute name:
	@MultipleAliasesComposedContextConfig(groovyScript = "groovyScript")
	protected static class GroovyMultipleAliasesComposedContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@MultipleAliasesComposedContextConfig(xmlFile = "xmlFile")
	protected static class XmlMultipleAliasesComposedContextConfigClass {
	}

	// Attribute value intentionally matches attribute name:
	@MultipleAliasesComposedContextConfig("value")
	protected static class ValueMultipleAliasesComposedContextConfigClass {
	}

}
