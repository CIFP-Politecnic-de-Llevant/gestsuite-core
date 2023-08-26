FROM maven:3-amazoncorretto-17 as develop-stage-core
WORKDIR /app

COPY /config/ /resources/

COPY /api/gestsuite-common/ /external/
RUN mvn clean compile install -f /external/pom.xml

COPY /api/gestsuite-core .
RUN mvn clean package -f pom.xml
ENTRYPOINT ["mvn","spring-boot:run","-f","pom.xml"]

FROM maven:3-amazoncorretto-17 as build-stage-core
WORKDIR /resources

COPY /api/gestsuite-common/ /external/
RUN mvn clean compile install -f /external/pom.xml


COPY /api/gestsuite-core .
RUN mvn clean package -f pom.xml

FROM amazoncorretto:17-alpine-jdk as production-stage-core
COPY --from=build-stage-core /resources/target/core-0.0.1-SNAPSHOT.jar core.jar
COPY /config/ /resources/
ENTRYPOINT ["java","-jar","/core.jar"]