TESTS=`./getActiveTests.sh`
rm -rf build/graalvm/META-INF
mkdir -p build/graalvm/META-INF/native-image
java -Dorg.graalvm.nativeimage.imagecode=agent -agentlib:native-image-agent=config-output-dir=build/graalvm/META-INF/native-image -cp $CP org.junit.platform.console.ConsoleLauncher $TESTS --details=summary --exclude-tag=uses-mockito
