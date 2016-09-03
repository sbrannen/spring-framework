
/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.concurrency;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.support.GenericXmlContextLoader;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;

/**
 * @author <a href="mailto:kristian@zeniorD0Tno">Kristian Rosenvold</a>
 */
public class MockContextLoader extends GenericXmlContextLoader {

	/**
	  * Activates a scope="session" in the beanfactory allowing us to register and retrieve session-scoped
	  * beans in the context. Spring 2.5
	  *
	  * @param context the parent scope
	  */
	protected void customizeContext(GenericApplicationContext context) {
		configureSessionInfrastructure();
		SessionScope testSessionScope = new SessionScope();
		context.getBeanFactory().registerScope("session", testSessionScope);
		RequestScope requestScope = new RequestScope();
		context.getBeanFactory().registerScope("request", requestScope);

	}

	/**
	 * Configures the necessary session-infrastructure needed to provide SessionScope.
	 */
	private void configureSessionInfrastructure() {
		initRequest();
	}

	private static void initRequest() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
		LocaleContextHolder.setLocale(request.getLocale(), true);
		RequestContextHolder.setRequestAttributes(attributes, true);

	}

	public static void requestCompleted() {
		@SuppressWarnings("unused")
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		// requestAttributes.requestCompleted();
		initRequest();
	}


	private static final String REQUEST_ATTRIBUTES_ATTRIBUTE = RequestContextListener.class.getName()
			+ ".REQUEST_ATTRIBUTES";

}
