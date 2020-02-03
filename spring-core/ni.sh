native-image \
  --no-server \
  -Dverbose=true \
  -H:+TraceClassInitialization \
  -H:Name=springcore \
  -H:+ReportExceptionStackTraces \
  --no-fallback \
-Dlog4j2.disable.jmx=true \
--initialize-at-build-time=org.springframework.core.SerializableTypeWrapper\$SerializableTypeProxy,org.apache.logging.log4j.core.LoggerContext,org.apache.logging.log4j.core.config,org.apache.logging.log4j.core.lookup,org.apache.logging.log4j.status,org.apache.logging.log4j.util,org.apache.logging.log4j.Level,org.apache.logging.log4j.core.util,org.apache.logging.log4j.message,org.apache.logging.log4j,org.apache.logging.log4j.MarkerManager\$Log4jMarker,org.apache.logging.log4j.core.impl,org.apache.logging.log4j.status,org.apache.logging.log4j.status.StatusLogger,org.apache.logging.log4j.spi.DefaultThreadContextMap,org.apache.logging.log4j.message.AbstractMessageFactory,org.apache.logging.log4j.core.selector.ClassLoaderContextSelector,org.apache.logging.log4j.core.config.LoggerConfig,org.apache.logging.log4j.simple.SimpleLogger,org.apache.logging.log4j.MarkerManager,org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory \
--initialize-at-build-time=org.springframework.core.type.AnnotationMetadataTests\$SubclassEnum\$1 \
--initialize-at-build-time=org.springframework.core.type.AnnotationMetadataTests\$SubclassEnum\$2 \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  -cp $CP:graal \
  org.junit.platform.console.ConsoleLauncher

# -H:ReflectionConfigurationResources=graal/reflect-config.json \
exit
--initialize-at-build-time=org.apache.commons.logging.LogAdapter \
  -H:ReflectionConfigurationResources=graal/aspects-reflect-config.json \

  -H:ResourceConfigurationFiles=graal/resource-config.json \
  -H:DynamicProxyConfigurationFiles=graal/proxies.json \


