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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} that retrieves a bean override instance from the
 * {@link BeanOverrideRegistrar} registered in the {@link BeanFactory}.
 *
 * @author Sam Brannen
 * @since 6.2
 * @param <T> the type of bean override instance returned from this factory
 */
@SuppressWarnings("rawtypes")
class BeanOverrideFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware {

	private final String beanName;

	private final Class<T> beanType;

	@Nullable
	private BeanOverrideRegistrar beanOverrideRegistrar;


	/**
	 * Create a new {@code BeanOverrideFactoryBean} for the given bean override
	 * name and type.
	 * @param beanName the name of the bean override instance
	 * @param beanType the type of the bean override instance
	 */
	BeanOverrideFactoryBean(String beanName, Class<T> beanType) {
		this.beanName = beanName;
		this.beanType = beanType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanOverrideRegistrar = beanFactory.getBean(BeanOverrideRegistrar.class);
	}

	@Override
	public Class<T> getObjectType() {
		return this.beanType;
	}

	@Override
	public T getObject() throws Exception {
		Assert.notNull(this.beanOverrideRegistrar, "BeanOverrideRegistrar must be available");
		return this.beanOverrideRegistrar.getBeanInstance(this.beanName, this.beanType);
	}

}
