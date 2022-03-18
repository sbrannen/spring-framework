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

package org.springframework.core.annotation;

import java.util.function.Predicate;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Configuration options for the search algorithm used in the {@link MergedAnnotations}
 * model.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public final class SearchAlgorithm {

	private final SearchStrategy searchStrategy;

	private final Predicate<Class<?>> searchEnclosingClass;

	private final RepeatableContainers repeatableContainers;

	private final AnnotationFilter annotationFilter;


	private SearchAlgorithm(SearchStrategy searchStrategy, Predicate<Class<?>> searchEnclosingClass,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		this.searchStrategy = searchStrategy;
		this.searchEnclosingClass = searchEnclosingClass;
		this.repeatableContainers = repeatableContainers;
		this.annotationFilter = annotationFilter;
	}

	public SearchStrategy searchStrategy() {
		return this.searchStrategy;
	}

	public Predicate<Class<?>> searchEnclosingClass() {
		return this.searchEnclosingClass;
	}

	public RepeatableContainers repeatableContainers() {
		return this.repeatableContainers;
	}

	public AnnotationFilter annotationFilter() {
		return this.annotationFilter;
	}

	public static Builder direct() {
		return new Builder(SearchStrategy.DIRECT);
	}

	public static Builder inheritedAnnotations() {
		return new Builder(SearchStrategy.INHERITED_ANNOTATIONS);
	}

	public static Builder superclass() {
		return new Builder(SearchStrategy.SUPERCLASS);
	}

	public static Builder typeHierarchy() {
		return new Builder(SearchStrategy.TYPE_HIERARCHY);
	}


	public static final class Builder {

		private final SearchStrategy searchStrategy;

		@Nullable
		private Predicate<Class<?>> searchEnclosingClass;

		private RepeatableContainers repeatableContainers = RepeatableContainers.standardRepeatables();

		private AnnotationFilter annotationFilter = AnnotationFilter.PLAIN;


		private Builder(SearchStrategy searchStrategy) {
			this.searchStrategy = searchStrategy;
		}

		public Builder searchEnclosingClass(Predicate<Class<?>> searchEnclosingClass) {
			Assert.notNull(searchEnclosingClass, "searchEnclosingClass predicate must not be null");
			this.searchEnclosingClass = searchEnclosingClass;
			return this;
		}

		public Builder repeatableContainers(RepeatableContainers repeatableContainers) {
			Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
			this.repeatableContainers = repeatableContainers;
			return this;
		}

		public Builder annotationFilter(AnnotationFilter annotationFilter) {
			Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
			this.annotationFilter = annotationFilter;
			return this;
		}

		public SearchAlgorithm build() {
			Assert.state(this.searchStrategy == SearchStrategy.TYPE_HIERARCHY || this.searchEnclosingClass == null,
				"A custom searchEnclosingClass predicate can only be combined with SearchStrategy.TYPE_HIERARCHY");

			Predicate<Class<?>> predicateToUse = this.searchEnclosingClass != null ? this.searchEnclosingClass : clazz -> false;
			return new SearchAlgorithm(this.searchStrategy, predicateToUse, this.repeatableContainers,
				this.annotationFilter);
		}

	}

}
