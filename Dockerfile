FROM eclipse-temurin:17-jre

WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} microserviciointegrador.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "microserviciointegrador.jar"]