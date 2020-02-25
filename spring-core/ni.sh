native-image \
	--no-server \
	--no-fallback \
	--allow-incomplete-classpath \
	--report-unsupported-elements-at-runtime \
	--initialize-at-build-time=org.apache.logging.log4j.core.LoggerContext,org.apache.logging.log4j.core.config,org.apache.logging.log4j.core.lookup,org.apache.logging.log4j.status,org.apache.logging.log4j.util,org.apache.logging.log4j.Level,org.apache.logging.log4j.core.util,org.apache.logging.log4j.message,org.apache.logging.log4j,org.apache.logging.log4j.MarkerManager\$Log4jMarker,org.apache.logging.log4j.core.impl,org.apache.logging.log4j.status,org.apache.logging.log4j.status.StatusLogger,org.apache.logging.log4j.spi.DefaultThreadContextMap,org.apache.logging.log4j.spi.ExtendedLogger,org.apache.logging.log4j.message.AbstractMessageFactory,org.apache.logging.log4j.core.selector.ClassLoaderContextSelector,org.apache.logging.log4j.core.config.LoggerConfig,org.apache.logging.log4j.simple.SimpleLogger,org.apache.logging.log4j.MarkerManager,org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory \
	--initialize-at-build-time=org.springframework.core.SerializableTypeWrapper\$SerializableTypeProxy \
	--initialize-at-build-time=org.springframework.core.type.AnnotationMetadataTests\$SubclassEnum\$1,org.springframework.core.type.AnnotationMetadataTests\$SubclassEnum\$2 \
	--initialize-at-run-time=org.apache.logging.log4j.core.async.AsyncLoggerConfigDisruptor,org.apache.logging.log4j.core.async.AsyncLoggerContext,org.apache.logging.log4j.core.config.yaml.YamlConfiguration,org.apache.logging.log4j.core.pattern.JAnsiTextRenderer \
	--initialize-at-run-time=org.apache.commons.logging.LogAdapter,org.apache.commons.logging.LogAdapter\$Log4jLog \
	-H:+TraceClassInitialization \
	-H:+ReportExceptionStackTraces \
	-H:+AddAllCharsets \
	-H:Name=build/spring-core-tests.bin \
	-Dverbose=true \
	-Dlog4j2.disable.jmx=true \
	-cp $CP:build/graalvm \
	org.junit.platform.console.ConsoleLauncher

exit

	--verbose \
	--initialize-at-run-time=net.bytebuddy,net.bytebuddy.description.method,net.bytebuddy.implementation,net.bytebuddy.implementation.bind,net.bytebuddy.implementation.bind.annotation,net.bytebuddy.implementation.bind.annotation.Argument\$BindingMechanic,net.bytebuddy.implementation.bind.annotation.Super,net.bytebuddy.implementation.bind.annotation.Super\$Instantiation \
	-H:DynamicProxyConfigurationFiles=proxy-config.json \
