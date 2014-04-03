/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.LinkedMultiValueMap;

import static org.springframework.core.annotation.AnnotationUtils.VALUE;

/**
 * Internal utility class used when reading annotations.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
abstract class AnnotationReadingVisitorUtils {

	public static AnnotationAttributes convertClassValues(ClassLoader classLoader, AnnotationAttributes original,
			boolean classValuesAsString) {

		if (original == null) {
			return null;
		}

		AnnotationAttributes result = new AnnotationAttributes(original.size());
		for (Map.Entry<String, Object> entry : original.entrySet()) {
			try {
				Object value = entry.getValue();
				if (value instanceof AnnotationAttributes) {
					value = convertClassValues(classLoader, (AnnotationAttributes) value, classValuesAsString);
				}
				else if (value instanceof AnnotationAttributes[]) {
					AnnotationAttributes[] values = (AnnotationAttributes[]) value;
					for (int i = 0; i < values.length; i++) {
						values[i] = convertClassValues(classLoader, values[i], classValuesAsString);
					}
				}
				else if (value instanceof Type) {
					value = (classValuesAsString ? ((Type) value).getClassName()
							: classLoader.loadClass(((Type) value).getClassName()));
				}
				else if (value instanceof Type[]) {
					Type[] array = (Type[]) value;
					Object[] convArray = (classValuesAsString ? new String[array.length] : new Class<?>[array.length]);
					for (int i = 0; i < array.length; i++) {
						convArray[i] = (classValuesAsString ? array[i].getClassName()
								: classLoader.loadClass(array[i].getClassName()));
					}
					value = convArray;
				}
				else if (classValuesAsString) {
					if (value instanceof Class) {
						value = ((Class<?>) value).getName();
					}
					else if (value instanceof Class[]) {
						Class<?>[] clazzArray = (Class[]) value;
						String[] newValue = new String[clazzArray.length];
						for (int i = 0; i < clazzArray.length; i++) {
							newValue[i] = clazzArray[i].getName();
						}
						value = newValue;
					}
				}
				result.put(entry.getKey(), value);
			}
			catch (Exception ex) {
				// Class not found - can't resolve class reference in annotation
				// attribute.
			}
		}
		return result;
	}

	/**
	 * Retrieve the merged attributes of the annotation of the given type, if any,
	 * from the supplied {@code attributesMap}.
	 * <p>Annotation attribute values appearing <em>lower</em> in the annotation
	 * hierarchy (i.e., closer to the declaring class) will override those
	 * defined <em>higher</em> in the annotation hierarchy.
	 * @param classLoader the {@code ClassLoader} to use to retrieve further metadata
	 * @param attributesMap the map of annotation attribute lists, keyed by
	 * annotation type name
	 * @param annotationType the name of the annotation type to look for
	 * @return the merged annotation attributes; or {@code null} if no matching
	 * annotation is present in the {@code attributesMap}
	 * @since 4.0.3
	 */
	public static AnnotationAttributes getMergedAnnotationAttributes(ClassLoader classLoader,
			LinkedMultiValueMap<String, AnnotationAttributes> attributesMap, String annotationType) {

		SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory(classLoader);

		// Get the unmerged list of attributes for the target annotation.
		List<AnnotationAttributes> attributesList = attributesMap.get(annotationType);
		if (attributesList == null || attributesList.isEmpty()) {
			return null;
		}

		// To start with, we populate the results with a copy of all attribute
		// values from the target annotation.
		AnnotationAttributes results = new AnnotationAttributes(attributesList.get(0));

		Set<String> overridableAttributeNames = new HashSet<String>(results.keySet());
		overridableAttributeNames.remove(VALUE);

		// Since the map is a LinkedMultiValueMap, we depend on the ordering of
		// elements in the map and reverse the order of the keys in order to traverse
		// "down" the annotation hierarchy.
		List<String> annotationTypes = new ArrayList<String>(attributesMap.keySet());
		Collections.reverse(annotationTypes);

		// No need to revisit the target annotation type:
		annotationTypes.remove(annotationType);

		for (String currentAnnotationType : annotationTypes) {
			// Ensure that the currentAnnotationType is meta-annotated with the target
			// annotationType (i.e., that its attributes are allowed to override those of
			// the target).
			if (isMetaAnnotated(currentAnnotationType, annotationType, factory)) {
				List<AnnotationAttributes> currentAttributesList = attributesMap.get(currentAnnotationType);
				if (currentAttributesList != null && !currentAttributesList.isEmpty()) {
					AnnotationAttributes currentAttributes = currentAttributesList.get(0);
					for (String overridableAttributeName : overridableAttributeNames) {
						Object value = currentAttributes.get(overridableAttributeName);
						if (value != null) {
							// Store the value, potentially overriding a value from an
							// attribute of the same name found higher in the annotation
							// hierarchy.
							results.put(overridableAttributeName, value);
						}
					}
				}
			}
		}

		return results;
	}

	/**
	 * Determine whether an annotation of the given target type is <em>present</em>
	 * on the candidate (i.e., whether the candidate is meta-annotated with the
	 * target).
	 * @param candidate the candidate annotation type
	 * @param target the annotation type to look for
	 * @param factory the {@code SimpleMetadataReaderFactory} to use to obtain
	 * {@link AnnotationMetadata} for the candidate
	 * @return whether a matching annotation is present
	 * @since 4.0.4
	 */
	private static boolean isMetaAnnotated(String candidate, String target, SimpleMetadataReaderFactory factory) {
		try {
			MetadataReader metadataReader = factory.getMetadataReader(candidate);
			AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
			return annotationMetadata.hasAnnotation(target);
		}
		catch (IOException e) {
			/* ignore */
		}
		return false;
	}

}
