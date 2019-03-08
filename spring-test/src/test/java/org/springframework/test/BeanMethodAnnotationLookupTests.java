
package org.springframework.test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
public class BeanMethodAnnotationLookupTests {

	@Autowired
	String foo;

	@Autowired
	String bar;


	@Test
	void test() {
		assertEquals("foo", this.foo);
		assertEquals("bar", this.bar);
	}


	@Configuration
	static class Config {

		@MyAnnotation
		@Bean
		String foo() {
			return "foo";
		}

		@Bean
		String bar() {
			return "bar";
		}

		@Bean
		static BeanFactoryPostProcessor myBeanFactoryPostProcessor() {
			return new MyBeanFactoryPostProcessor();
		}

	}

	static class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			Class<? extends Annotation> annotationType = MyAnnotation.class;

			Arrays.stream(beanFactory.getBeanDefinitionNames())//
					.map(beanFactory::getBeanDefinition)//
					.filter(bd -> !bd.isAbstract())//
					.filter(bd -> bd.getFactoryMethodName() != null)//
					.filter(bd -> bd.getFactoryBeanName() != null)//
					.filter(bd -> isBeanMethodAnnotated(beanFactory, bd, annotationType))//
					.forEach(bd -> System.out.println("@Bean method " + bd.getFactoryMethodName()
							+ " is annotated with @" + annotationType.getSimpleName() + "."));
		}

		private boolean isBeanMethodAnnotated(ConfigurableListableBeanFactory beanFactory,
				BeanDefinition beanDefinition, Class<? extends Annotation> annotationType) {

			BeanDefinition factoryBeanDefinition = beanFactory.getBeanDefinition(beanDefinition.getFactoryBeanName());

			if (factoryBeanDefinition instanceof AbstractBeanDefinition) {
				AbstractBeanDefinition abd = (AbstractBeanDefinition) factoryBeanDefinition;
				if (abd.hasBeanClass()) {
					Class<?> factoryClass = ClassUtils.getUserClass(abd.getBeanClass());
					String factoryMethodName = beanDefinition.getFactoryMethodName();
					Method factoryMethod = ReflectionUtils.findMethod(factoryClass, factoryMethodName);
					return AnnotatedElementUtils.isAnnotated(factoryMethod, Bean.class)
							&& AnnotatedElementUtils.isAnnotated(factoryMethod, annotationType);
				}
			}

			return false;
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyAnnotation {
	}

}
