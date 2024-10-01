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

package org.springframework.test.context.bean.override;

import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} that mimics a factory bean capable of creating a bean
 * override instance.
 *
 * @author Sam Brannen
 * @since 6.2
 * @param <T> the type of bean override instance returned from this factory
 */
class BeanOverrideFactoryBean<T> implements FactoryBean<T> {

	private final Class<T> beanType;

	/**
	 * Create a new {@code BeanOverrideFactoryBean} for the given bean override type.
	 * @param beanType the type of the bean override instance
	 */
	BeanOverrideFactoryBean(Class<T> beanType) {
		this.beanType = beanType;
	}

	@Override
	public Class<T> getObjectType() {
		return this.beanType;
	}

	@Override
	public T getObject() throws Exception {
		throw new UnsupportedOperationException(
				"A BeanOverrideFactoryBean should not be used to create a bean override instance.");
	}

}
