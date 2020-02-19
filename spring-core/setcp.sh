export CP=`cat cp.txt | grep "^/" | tr '\n' ':'`
export CP=$CP:junit-platform-console-standalone-1.6.0.jar
export CP=$CP:./build/classes/java/test:./build/classes/java/main:./build/resources/test
