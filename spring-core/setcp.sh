export CP=`cat cp.txt | grep "^/" | tr '\n' ':'`
export CP=$CP:./build/resources/test:./build/resources/testFixtures:./build/resources/main:./build/classes/java/test:./build/classes/kotlin/test:./build/classes/java/testFixtures:./build/classes/java/main:./build/classes/kotlin/main
