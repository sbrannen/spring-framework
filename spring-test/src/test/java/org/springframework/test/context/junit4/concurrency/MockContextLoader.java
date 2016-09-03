/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4.concurrency;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.support.GenericXmlContextLoader;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;

/**
 * @author <a href="mailto:kristian@zeniorD0Tno">Kristian Rosenvold</a>
 */
public class MockContextLoader extends GenericXmlContextLoader {

	private static final Map<String, LocalAttrs> attrCache = new HashMap<String, LocalAttrs>();


	/**
	 * Activates a scope="session" in the {@code BeanFactory} allowing us to
	 * register and retrieve session-scoped beans in the context.
	 *
	 * <p>Spring 2.5
	 */
	protected void customizeContext(GenericApplicationContext context) {
		configureSessionInfrastructure();
		SessionScope testSessionScope = new SessionScope();
		context.getBeanFactory().registerScope("session", testSessionScope);
		RequestScope requestScope = new RequestScope();
		context.getBeanFactory().registerScope("request", requestScope);
	}

	/**
	 * Activates a scope="session" in the {@code BeanFactory} allowing us to
	 * register and retrieve session-scoped beans in the context.
	 *
	 * <p>Spring 3.0 adapted
	 */
	protected void customizeContext(GenericApplicationContext context, String cacheKey) {
		customizeContext(context);

		final LocalAttrs value = new LocalAttrs(RequestContextHolder.getRequestAttributes(),
			LocaleContextHolder.getLocale());
		System.out.println("cacheKey = " + cacheKey);
		synchronized (attrCache) {
			attrCache.put(cacheKey, value);
		}
	}

	public void activateForThread(ApplicationContext applicationContext, String key) {
		synchronized (attrCache) {
			LocalAttrs localAttrs = attrCache.get(key);
			// We are in a later thread/invocation. Need to set up spring context for this
			// thread.
			if (localAttrs == null) {
				System.out.println("You cannot call this method before getApplicationContext, faking it ?");
				return;
			}
			LocaleContextHolder.setLocale(localAttrs.getLocale(), true);
			RequestContextHolder.setRequestAttributes(localAttrs.getCopyOfRequestAttributes(), true);
		}
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


	class LocalAttrs {

		private RequestAttributes requestAttributes;
		private Locale locale;


		LocalAttrs(RequestAttributes requestAttributes, Locale locale) {
			this.requestAttributes = requestAttributes;
			this.locale = locale;
		}

		public RequestAttributes getCopyOfRequestAttributes() {
			if (requestAttributes instanceof ServletRequestAttributes) {
				ServletRequestAttributes original = (ServletRequestAttributes) requestAttributes;
				ServletRequestAttributes result;
				HttpServletRequest nextRequest;
				if (original.getRequest() instanceof MockHttpServletRequest) {
					// Mock request, just clone it.
					nextRequest = new MockHttpServletRequest(); // Maybe need to clone.
				}
				else {
					nextRequest = original.getRequest();
				}

				result = new ServletRequestAttributes(nextRequest);

				nextRequest.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE,
					original.getRequest().getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE));

				return result;
			}
			return requestAttributes; // Maybe
		}

		public Locale getLocale() {
			return locale;
		}
	}

}
