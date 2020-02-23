TESTS=`./getActiveTests.sh`
./build/spring-core-tests.bin --details=summary --exclude-tag="uses-mockito | uses-serialization" $TESTS
