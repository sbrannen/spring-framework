/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core;

/**
 * A common delegate for detecting a GraalVM native image environment.
 *
 * <p>Only intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 5.1
 */
public abstract class GraalVmDetector {

	/**
	 * As of GraalVM 20, GraalVM sets the {@code org.graalvm.nativeimage.imagecode}
	 * JVM system property to either {@code buildtime} or {@code runtime}; however,
	 * the Spring Team supplies a custom value of {@code agent} when executing
	 * with the GraalVM agent.
	 * <p>The GraalVM team may later modify the agent to set the property itself.
	 * <p>In any case, checking that the value is non-null meets our needs for
	 * all three use cases.
	 * @see <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">ImageInfo</a>
	 */
	private static final boolean imageCode = (System.getProperty("org.graalvm.nativeimage.imagecode") != null);


	/**
	 * Determine whether the current runtime environment is executing while
	 * running with the GraalVM agent, while building a native image, or within
	 * a native image.
	 */
	public static boolean inImageCode() {
		return imageCode;
	}

}
