FROM maven:amazoncorretto
#FROM openjdk:11-jdk-slim

WORKDIR /resources
COPY ../gestsuite-common/target/common-0.0.1-SNAPSHOT.jar .
COPY ../gestsuite-common/pom.xml .
RUN mvn install:install-file -Dfile=/resources/common-0.0.1-SNAPSHOT.jar -DpomFile=/resources/pom.xml
#WORKDIR /app
#RUN ./app/mvnw install:install-file -Dfile=/resources/common-0.0.1-SNAPSHOT.jar -DpomFile=/resources/pom.xml


#RUN ./mvnw clean compile install package