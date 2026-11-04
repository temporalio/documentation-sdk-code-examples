FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=build /workspace/target/worker.jar /app/worker.jar
# @@@SNIPSTART java-cloud-run-dockerfile-cmd
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/worker.jar"]
# @@@SNIPEND
