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

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.LocalEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HibernateTransactionManager}, in particular its participation
 * in an {@link EntityManagerHolder} that has already been bound for the same
 * underlying {@code SessionFactory}/{@code EntityManagerFactory} instance -- for
 * example by {@link org.springframework.orm.jpa.JpaTransactionManager} or by an
 * open-EntityManager-in-view filter/interceptor.
 *
 * @author Sam Brannen
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewTests
 */
class HibernateTransactionManagerTests {

	private final EntityManagerFactory entityManagerFactory = createEntityManagerFactory();

	private final SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

	private final HibernateTransactionManager transactionManager = new HibernateTransactionManager(this.sessionFactory);


	@AfterEach
	void closeEntityManagerFactory() {
		this.entityManagerFactory.close();
	}

	@Test
	void plainTransactionWithoutPreBoundResource() {
		assertThat(TransactionSynchronizationManager.hasResource(this.sessionFactory)).isFalse();

		TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
		assertThat(TransactionSynchronizationManager.getResource(this.sessionFactory)).isInstanceOf(SessionHolder.class);

		this.transactionManager.commit(status);
		assertThat(TransactionSynchronizationManager.hasResource(this.sessionFactory)).isFalse();
	}

	@Test
	void participatesInEntityManagerHolderBoundByOpenEntityManagerInView() {
		// Simulate OpenEntityManagerInViewFilter/Interceptor: bind a plain EntityManagerHolder,
		// not yet synchronized with any transaction, before the @Transactional boundary.
		EntityManager entityManager = this.entityManagerFactory.createEntityManager();
		EntityManagerHolder entityManagerHolder = new EntityManagerHolder(entityManager);
		TransactionSynchronizationManager.bindResource(this.entityManagerFactory, entityManagerHolder);

		try {
			TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

			// HibernateTransactionManager must have replaced the EntityManagerHolder with a
			// SessionHolder for the SAME underlying Session, rather than throwing a ClassCastException.
			Object resource = TransactionSynchronizationManager.getResource(this.sessionFactory);
			assertThat(resource).isInstanceOf(SessionHolder.class);
			assertThat(((SessionHolder) resource).getSession()).isSameAs(entityManager.unwrap(Session.class));

			this.transactionManager.commit(status);
		}
		finally {
			// Simulate OpenEntityManagerInViewFilter/Interceptor's own cleanup: it must still be
			// able to find (and unbind/close) the very same EntityManagerHolder it originally bound.
			Object restored = TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
			assertThat(restored).isSameAs(entityManagerHolder);
			entityManager.close();
		}
	}

	@Test
	void participatesInEntityManagerHolderAndRestoresItAfterFailedBegin() {
		EntityManager entityManager = this.entityManagerFactory.createEntityManager();
		EntityManagerHolder entityManagerHolder = new EntityManagerHolder(entityManager);
		TransactionSynchronizationManager.bindResource(this.entityManagerFactory, entityManagerHolder);
		// Close the underlying Session up front so that HibernateTransactionManager's
		// doBegin() fails once it tries to start a Hibernate transaction on it.
		entityManager.close();

		try {
			assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
					this.transactionManager.getTransaction(new DefaultTransactionDefinition()));

			// The original EntityManagerHolder must have been restored, unchanged.
			assertThat(TransactionSynchronizationManager.getResource(this.entityManagerFactory))
					.isSameAs(entityManagerHolder);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
		}
	}

	private static EntityManagerFactory createEntityManagerFactory() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
		dataSource.setUrl("jdbc:hsqldb:mem:" + HibernateTransactionManagerTests.class.getSimpleName());
		dataSource.setUsername("sa");
		dataSource.setPassword("");

		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setDatabase(Database.HSQL);
		adapter.setGenerateDdl(true);

		LocalEntityManagerFactoryBean factoryBean = new LocalEntityManagerFactoryBean();
		factoryBean.setPersistenceUnitName("Person");
		factoryBean.setPackagesToScan("org.springframework.orm.jpa.domain");
		factoryBean.setDataSource(dataSource);
		factoryBean.setJpaVendorAdapter(adapter);
		factoryBean.afterPropertiesSet();
		return factoryBean.getObject();
	}

}
