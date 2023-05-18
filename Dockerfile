FROM maven:3-amazoncorretto-11 as build-stage-core
WORKDIR /resources

COPY /api/gestsuite-common/ /external/
RUN mvn clean compile install -f /external/pom.xml


COPY /api/gestsuite-core .
RUN mvn clean package -f pom.xml

FROM amazoncorretto:11-alpine-jdk as production-stage-core
COPY --from=build-stage-core /resources/target/core-0.0.1-SNAPSHOT.jar core.jar
COPY /config/iesmanacor-e0d4f26d9c2c.json /resources/iesmanacor-e0d4f26d9c2c.json
ENTRYPOINT ["java","-jar","/core.jar"]