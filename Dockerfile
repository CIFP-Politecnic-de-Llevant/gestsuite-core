FROM maven:3-amazoncorretto-11 as common-stage

WORKDIR /resources

COPY /api/gestsuite-common/pom.xml .
RUN mvn clean package -f pom.xml

FROM maven:3-amazoncorretto-11 as build-stage
WORKDIR /resources

COPY /api/gestsuite-common/ /external/
RUN mvn clean install -f /external/pom.xml


COPY /api/gestsuite-core .
RUN mvn clean package -f pom.xml

FROM amazoncorretto:11-alpine-jdk as production-stage
COPY --from=build-stage /resources/target/core-0.0.1-SNAPSHOT.jar core.jar
COPY /config/iesmanacor-e0d4f26d9c2c.json /resources/iesmanacor-e0d4f26d9c2c.json
ENTRYPOINT ["java","-jar","/core.jar"]