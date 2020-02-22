export CP=`cat cp.txt | grep "^/" | tr '\n' ':'`
export CP=$CP:junit-platform-console-standalone-1.6.0.jar

# Everything:
export CP=$CP:./build/resources/test:./build/resources/main:./build/classes/java/test:./build/classes/kotlin/test:./build/classes/java/testFixtures:./build/classes/java/main:./build/classes/kotlin/main

## Everyting minus Kotlin test classes:
# export CP=$CP:./build/resources/test:./build/classes/java/test:./build/classes/java/testFixtures:./build/classes/java/main:./build/classes/kotlin/main

## Everyting minus Kotlin test classes and Kotlin main classes (i.e., Kotlin coroutine support):
# export CP=$CP:./build/resources/test:./build/classes/java/test:./build/classes/java/testFixtures:./build/classes/java/main
