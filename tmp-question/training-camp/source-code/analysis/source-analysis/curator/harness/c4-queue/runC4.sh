#!/bin/bash
# C-3 harness 运行脚本 (真 ZK 内嵌)
M2=/data/.m2/repository
CP=""
for jar in \
  $M2/org/apache/curator/curator-framework/5.8.0/curator-framework-5.8.0.jar \
  $M2/org/apache/curator/curator-client/5.8.0/curator-client-5.8.0.jar \
  $M2/org/apache/curator/curator-recipes/5.8.0/curator-recipes-5.8.0.jar \
  $M2/org/apache/curator/curator-test/5.9.0/curator-test-5.9.0.jar \
  $M2/org/apache/zookeeper/zookeeper/3.9.5/zookeeper-3.9.5.jar \
  $M2/org/apache/zookeeper/zookeeper-jute/3.9.5/zookeeper-jute-3.9.5.jar \
  $M2/com/google/guava/guava/33.4.8-jre/guava-33.4.8-jre.jar \
  $M2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar \
  $M2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar \
  $M2/io/netty/netty-handler/4.1.82.Final/netty-handler-4.1.82.Final.jar \
  $M2/io/netty/netty-transport/4.1.82.Final/netty-transport-4.1.82.Final.jar \
  $M2/io/netty/netty-common/4.1.82.Final/netty-common-4.1.82.Final.jar \
  $M2/io/netty/netty-buffer/4.1.82.Final/netty-buffer-4.1.82.Final.jar \
  $M2/io/netty/netty-resolver/4.1.82.Final/netty-resolver-4.1.82.Final.jar \
  $M2/io/netty/netty-codec/4.1.82.Final/netty-codec-4.1.82.Final.jar \
  $M2/commons-io/commons-io/2.7/commons-io-2.7.jar \
  $M2/org/apache/commons/commons-lang3/3.9/commons-lang3-3.9.jar \
  $M2/commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.jar \
  $M2/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar \
  $M2/com/fasterxml/jackson/core/jackson-databind/2.13.4.2/jackson-databind-2.13.4.2.jar \
  $M2/com/fasterxml/jackson/core/jackson-core/2.13.4/jackson-core-2.13.4.jar \
  $M2/com/fasterxml/jackson/core/jackson-annotations/2.13.4/jackson-annotations-2.13.4.jar \
  $M2/org/xerial/snappy/snappy-java/1.1.7/snappy-java-1.1.7.jar \
  $M2/org/apache/yetus/audience-annotations/0.12.0/audience-annotations-0.12.0.jar \
  $M2/jline/jline/2.14.6/jline-2.14.6.jar \
  $M2/io/dropwizard/metrics/metrics-core/4.2.16/metrics-core-4.2.16.jar \
; do
  [ -f "$jar" ] && CP="$CP:$jar" || echo "MISSING: $jar"
done

echo "== 编译 =="
javac -cp "$CP" -d /tmp/c4harness HarnessC4.java || exit 1
echo "== 运行 =="
java -cp "/tmp/c4harness$CP" -Dorg.slf4j.simpleLogger.defaultLogLevel=warn HarnessC4
