FROM maven:3-jdk-12-alpine

WORKDIR /usr/src/myapp

COPY pom.xml /usr/src/myapp/pom.xml
COPY src/main/java/com/birchbox/app/*Application.java /usr/src/myapp/src/main/java/com/birchbox/app/
RUN mvn package -DskipTests

COPY . /usr/src/myapp
RUN mvn package -o -DskipTests

FROM openjdk:11
COPY --from=0 /usr/src/myapp/target/*.jar /app.jar
ENTRYPOINT java -Xmx2048m -jar /app.jar