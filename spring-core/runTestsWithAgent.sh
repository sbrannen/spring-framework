TESTS=`./getActiveTests.sh`
rm -rf build/graalvm/META-INF
mkdir -p build/graalvm/META-INF/native-image
java -agentlib:native-image-agent=config-output-dir=build/graalvm/META-INF/native-image -Dorg.graalvm.nativeimage.imagecode=agent -cp $CP org.junit.platform.console.ConsoleLauncher --details=summary --exclude-tag="uses-blockhound | uses-kotlin-coroutines | uses-kotlin-reflection | uses-mockito | uses-mockk" $TESTS
