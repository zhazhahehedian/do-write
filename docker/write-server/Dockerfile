FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# 先拷贝 POM 以便利用缓存下载依赖
COPY pom.xml ./pom.xml
COPY write-common/pom.xml ./write-common/pom.xml
COPY write-server/pom.xml ./write-server/pom.xml

RUN mvn -q -DskipTests dependency:go-offline

# 再拷贝源码并构建
COPY write-common/src ./write-common/src
COPY write-server/src ./write-server/src

RUN mvn -q -pl write-server -am -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/write-server/target/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

EXPOSE 8099

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
