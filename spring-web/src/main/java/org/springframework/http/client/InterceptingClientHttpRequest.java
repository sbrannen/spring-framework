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

package org.springframework.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.StreamUtils;

/**
 * Wrapper for a {@link ClientHttpRequest} that has support for {@link ClientHttpRequestInterceptor
 * ClientHttpRequestInterceptors}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequestFactory requestFactory;

	private final List<ClientHttpRequestInterceptor> interceptors;

	private final HttpMethod method;

	private final URI uri;


	protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
			List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.method = method;
		this.uri = uri;
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		InterceptingRequestExecution requestExecution = new InterceptingRequestExecution();
		return requestExecution.execute(this, bufferedOutput);
	}


	private class InterceptingRequestExecution implements ClientHttpRequestExecution {

		private final Iterator<ClientHttpRequestInterceptor> iterator;

		public InterceptingRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
			if (this.iterator.hasNext()) {
				ClientHttpRequestInterceptor nextInterceptor = this.iterator.next();
				return nextInterceptor.intercept(request, body, this);
			}
			else {
				HttpMethod method = request.getMethod();
				ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), method);
				request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));
				request.getAttributes().forEach((key, value) -> delegate.getAttributes().put(key, value));
				if (body.length > 0) {
					long contentLength = delegate.getHeaders().getContentLength();
					if (contentLength > -1 && contentLength != body.length) {
						delegate.getHeaders().setContentLength(body.length);
					}
					if (delegate instanceof StreamingHttpOutputMessage streamingOutputMessage) {
						streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
							@Override
							public void writeTo(OutputStream outputStream) throws IOException {
								StreamUtils.copy(body, outputStream);
							}

							@Override
							public boolean repeatable() {
								return true;
							}
						});
					}
					else {
						StreamUtils.copy(body, delegate.getBody());
					}
				}
				return delegate.execute();
			}
		}
	}

}
