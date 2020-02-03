TESTS=`./getActiveTests.sh`
rm -rf graal/META-INF
mkdir -p graal/META-INF/native-image
java -agentlib:native-image-agent=config-output-dir=graal/META-INF/native-image -cp $CP org.junit.platform.console.ConsoleLauncher $TESTS
