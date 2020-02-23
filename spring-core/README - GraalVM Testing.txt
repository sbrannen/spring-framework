==========================================================================================
=== INSTRUCTIONS
==========================================================================================

1. Download the Java 8 version of GraalVM.

   We are currently using the following version.

   https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-20.0.0

------------------------------------------------------------------------------------------

2. Unpack the archive and set the JAVA_HOME environment variable to point to it. Then
   modify the PATH environment variable to include JAVA_HOME/bin. Then run `gu install
   native-image` to download the native-image command.

   For example, if you are developing on Mac OS and you unpacked the archive into
   /opt/graalvm/graalvm-ce-java8-20.0.0, the following commands should work.

   export GRAALVM_HOME=/opt/graalvm/graalvm-ce-java8-20.0.0/Contents/Home
   export PATH=$GRAALVM_HOME/bin:$PATH
   export JAVA_HOME=$GRAALVM_HOME
   
   Executing `java -version` should then display something similar to the following.

     openjdk version "1.8.0_242"
     OpenJDK Runtime Environment (build 1.8.0_242-b06)
     OpenJDK 64-Bit Server VM GraalVM CE 20.0.0 (build 25.242-b06-jvmci-20.0-b02, mixed mode)

------------------------------------------------------------------------------------------

3. Download the junit-platform-console-standalone-1.6.0.jar and place it in the spring-core
   module's directory.

   https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.6.0/junit-platform-console-standalone-1.6.0.jar

------------------------------------------------------------------------------------------

4. Determine the runtime classpath necessary to execute all tests.

   As a temporary workaround, we are adding the following custom Gradle task to build.gradle.

   task printClasspath {
     doLast {
       configurations.runtime.each { println it }
       configurations.testRuntime.each { println it }
       configurations.optional.each { println it }
     }
   }

   Once that is in place, execute `../gradlew printClasspath > cp.txt` within the
   spring-core directory to create a file named `cp.txt` that contains classpath entries
   for dependencies of spring-core. This file will be used by shell scripts discussed
   later.

   Currently we must manually remove the following from `cp.txt`:

   - org.jetbrains.kotlinx/kotlinx-coroutines-core
   - org.jetbrains.kotlinx/kotlinx-coroutines-reactive
   - org.jetbrains.kotlinx/kotlinx-coroutines-reactor

------------------------------------------------------------------------------------------

5. Execute `. ./setcp.sh` to set the CP environment variable that contains the classpath
   entries used by later scripts. Note that the initial "." is necessary in order to have
   the script evaluated in the current shell.

------------------------------------------------------------------------------------------

6. Run the tests using the standard Gradle test task in order to get an idea of how many
   tests we should be expecting to run if things work.

   ../gradlew test

   open build/reports/tests/test/index.html

------------------------------------------------------------------------------------------

7. Determine the list of tests to run and store it in `tests.txt`.

   This will later be automated, but for the time being we have used the following to
   generate the existing `tests.txt` file.

   find build/classes/*/test -name "*Tests.class" | cut -c 25- | sed 's/.*\/org/org/g' | sed 's/\//./g' | sed 's/.class$//' > tests.txt

   Tests that are known not to pass when executed in a native image can be excluded by
   prepending a hashtag `#` to the beginning of the corresponding line in `tests.txt`.

------------------------------------------------------------------------------------------

8. `getActiveTests.sh` can be used to convert `tests.txt` into a single string that can be
   passed to JUnit's ConsoleLauncher. This script will be used by later scripts.

------------------------------------------------------------------------------------------

9. Execute `runTestsWithAgent.sh` to run the tests with the GraalVM JVM agent.

------------------------------------------------------------------------------------------

10. Execute the `ni.sh` script which compiles a `spring-core-tests.bin` native image
    executable in the `build` directory using the generated configuration.

------------------------------------------------------------------------------------------

11. Execute `runTestsInNativeImage.sh` to run the tests in the native image.

------------------------------------------------------------------------------------------




==========================================================================================
=== NOTES
==========================================================================================

AnnotatedElementUtilsTests:

- javaxAnnotationTypeViaFindMergedAnnotation() requires the following in proxy-config:
    ["javax.annotation.Resource", "org.springframework.core.annotation.SynthesizedAnnotation"]

------------------------------------------------------------------------------------------

DefaultConversionServiceTests:

- convertObjectToStringWithJavaTimeOfMethodPresent(): fails because the GraalVM agent does
	not detect that it needs to include superclasses in reflect-config.json when
	Class#getMethod is invoked (as in org.springframework.util.ClassUtils.getStaticMethod()).
	This may be a bug in the GraalVM agent. As a workaround, we have added an
	`allDeclaredMethods` entry to reflect-config.json for java.time.ZoneRegion so that the
	`of(String)` method defined in ZoneId (ZoneRegion's superclass) is visible via reflection.

- convertStringToTimezone(): fails because it uses java.util.TimeZone.getTimeZone(String)
	with a custom Zone ID which doesn't seem to be properly supported in a GraalVM native
	image (perhaps due to the use of sun.util.calendar.ZoneInfoFile.getCustomTimeZone()).
	Assertion failure: Expecting: <"GMT"> to be equal to: <"GMT+02:00"> but was not.
	We have circumvented this by aborting this test method when running in a native image.

------------------------------------------------------------------------------------------





