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

package org.springframework.core.testfixture.junit.platform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * JUnit Platform {@link TestExecutionListener} that tracks the test classes
 * that were executed and writes their fully qualified class names to a file
 * (currently hard coded to {@code ./build/test_classes.txt"}).
 *
 * @author Sam Brannen
 * @since 5.2.5
 */
public class TestClassTrackingTestExecutionListener implements TestExecutionListener {

	private static final String FILE_NAME = "test_classes.txt";

	private final List<String> testClassNames = new ArrayList<>();


	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testIdentifier.isContainer()) {
			testIdentifier.getSource()//
					.filter(ClassSource.class::isInstance)//
					.map(ClassSource.class::cast)//
					.map(ClassSource::getClassName)//
					.ifPresent(this.testClassNames::add);
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		File file = getFile();
		if (file == null) {
			return;
		}

		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)), true)) {
			this.testClassNames.stream().sorted().forEach(writer::println);
		}
		catch (IOException ex) {
			System.err.println("Failed to write test class names to file " + file);
			ex.printStackTrace(System.err);
		}
	}

	private static File getFile() {
		File buildDir = new File(".", "build");
		if (!buildDir.exists() && !buildDir.mkdirs()) {
			System.err.println("Failed to create directory " + buildDir);
			return null;
		}

		File file = new File(buildDir, FILE_NAME);
		if (file.exists() && !file.delete()) {
			System.err.println("Failed to delete file " + file);
			return null;
		}

		try {
			file.createNewFile();
		}
		catch (IOException ex) {
			System.err.println("Failed to create file " + file);
			ex.printStackTrace(System.err);
			return null;
		}

		return file;
	}

}
