TESTS=`./getActiveTests.sh`
./build/spring-core-tests.bin --details=summary --exclude-tag="uses-blockhound | uses-kotlin-coroutines | uses-kotlin-reflection | uses-mockito | uses-mockk" $TESTS
