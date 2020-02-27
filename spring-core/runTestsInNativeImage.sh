TESTS=`./getActiveTests.sh`
./build/spring-core-tests.bin --details=summary --exclude-tag="uses-kotlin-coroutines | uses-kotlin-reflection | uses-mockito | uses-mockk | uses-security-manager | uses-serialization" $TESTS
