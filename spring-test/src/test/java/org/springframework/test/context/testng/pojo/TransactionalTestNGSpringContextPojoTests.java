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

package org.springframework.test.context.testng.pojo;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.SpringListener;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

/**
 * @author Sam Brannen
 * @since 7.0
 */
@Listeners(SpringListener.class)
@ContextConfiguration
@Transactional
// @org.testng.annotations.Ignore
class TransactionalTestNGSpringContextPojoTests implements BeanNameAware, InitializingBean {

	private static final String JANE = "jane";
	private static final String SUE = "sue";
	private static final String YODA = "yoda";

	private static final int NUM_TESTS = 8;
	private static final int NUM_TX_TESTS = 1;

	private static int numSetUpCalls = 0;
	private static int numSetUpCallsInTransaction = 0;
	private static int numTearDownCalls = 0;
	private static int numTearDownCallsInTransaction = 0;


	final JdbcTemplate jdbcTemplate = new JdbcTemplate();

	@Autowired
	ApplicationContext applicationContext;

	@Autowired
	DataSource dataSource;

	Employee employee;

	@Autowired
	Pet pet;

	@Autowired(required = false)
	Long nonrequiredLong;

	@Resource
	String foo;

	String bar;

	String beanName;

	boolean beanInitialized = false;


	@Autowired
	private void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Resource
	private void setBar(String bar) {
		this.bar = bar;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void afterPropertiesSet() {
		this.beanInitialized = true;
		this.jdbcTemplate.setDataSource(this.dataSource);
	}


	@BeforeClass
	void beforeClass() {
		System.err.println("#### @BeforeClass -- setUp() within transaction: " + numSetUpCallsInTransaction);
		numSetUpCalls = 0;
		numSetUpCallsInTransaction = 0;
		numTearDownCalls = 0;
		numTearDownCallsInTransaction = 0;
	}

	@AfterClass
	void afterClass() {
		System.err.println("#### @AfterClass  -- setUp() within transaction: " + numSetUpCallsInTransaction);
		assertThat(numSetUpCalls).as("number of calls to setUp().").isEqualTo(NUM_TESTS);
		assertThat(numSetUpCallsInTransaction).as("number of calls to setUp() within a transaction.").isEqualTo(NUM_TX_TESTS);
		assertThat(numTearDownCalls).as("number of calls to tearDown().").isEqualTo(NUM_TESTS);
		assertThat(numTearDownCallsInTransaction).as("number of calls to tearDown() within a transaction.").isEqualTo(NUM_TX_TESTS);
	}

	@BeforeMethod
	void setUp() {
		numSetUpCalls++;
		if (isActualTransactionActive()) {
			numSetUpCallsInTransaction++;
		}
		assertNumRowsInPersonTable((isActualTransactionActive() ? 2 : 1), "before a test method");
		System.err.println("#### @BeforeMethod -- setUp() within transaction: " + numSetUpCallsInTransaction);
	}

	@AfterMethod
	void tearDown() {
		numTearDownCalls++;
		if (isActualTransactionActive()) {
			numTearDownCallsInTransaction++;
		}
		assertNumRowsInPersonTable((isActualTransactionActive() ? 4 : 1), "after a test method");
		System.err.println("#### @AfterMethod  -- setUp() within transaction: " + numSetUpCallsInTransaction);
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertNumRowsInPersonTable(1, "before a transactional test method");
		assertAddPerson(YODA);
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
		assertNumRowsInPersonTable(1, "after a transactional test method");
	}


	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyBeanNameSet() {
		assertThatTransaction().isNotActive();
		assertThat(this.beanName)
			.as("The bean name of this test instance should have been set to the fully qualified class name due to BeanNameAware semantics.")
			.startsWith(getClass().getName());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyApplicationContextSet() {
		assertThatTransaction().isNotActive();
		assertThat(applicationContext)
			.as("The application context should have been set due to ApplicationContextAware semantics.")
			.isNotNull();
		Employee employeeBean = (Employee) applicationContext.getBean("employee");
		assertThat(employeeBean.getName()).as("employee's name.").isEqualTo("John Smith");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyBeanInitialized() {
		assertThatTransaction().isNotActive();
		assertThat(beanInitialized)
			.as("This test instance should have been initialized due to InitializingBean semantics.")
			.isTrue();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyAnnotationAutowiredFields() {
		assertThatTransaction().isNotActive();
		assertThat(nonrequiredLong).as("The nonrequiredLong field should NOT have been autowired.").isNull();
		assertThat(pet).as("The pet field should have been autowired.").isNotNull();
		assertThat(pet.getName()).as("pet's name.").isEqualTo("Fido");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyAnnotationAutowiredMethods() {
		assertThatTransaction().isNotActive();
		assertThat(employee).as("The setEmployee() method should have been autowired.").isNotNull();
		assertThat(employee.getName()).as("employee's name.").isEqualTo("John Smith");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyResourceAnnotationInjectedFields() {
		assertThatTransaction().isNotActive();
		assertThat(foo).as("The foo field should have been injected via @Resource.").isEqualTo("Foo");
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void verifyResourceAnnotationInjectedMethods() {
		assertThatTransaction().isNotActive();
		assertThat(bar).as("The setBar() method should have been injected via @Resource.").isEqualTo("Bar");
	}

	@Test
	void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertAddPerson(JANE);
		assertAddPerson(SUE);
		assertNumRowsInPersonTable(4, "in modifyTestDataWithinTransaction()");
	}


	private int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
	}

	private int createPerson(String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertThat(countRowsInTable("person"))
				.as("the number of rows in the person table (" + testState + ").")
				.isEqualTo(expectedNumRows);
	}

	private void assertAddPerson(String name) {
		assertThat(createPerson(name)).as("Adding '" + name + "'").isEqualTo(1);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		Employee employee() {
			return new Employee("John Smith");
		}

		@Bean
		Pet pet() {
			return new Pet("Fido");
		}

		@Bean
		String foo() {
			return "Foo";
		}

		@Bean
		String bar() {
			return "Bar";
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.generateUniqueName(true)
					.addScript("classpath:/org/springframework/test/jdbc/schema.sql")
					.addScript("classpath:/org/springframework/test/jdbc/data.sql")
					.build();
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}
	}

}
