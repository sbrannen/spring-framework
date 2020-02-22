TESTS=`./getActiveTests.sh`
./build/spring-core-tests.bin $TESTS --details=summary --exclude-tag=uses-mockito
