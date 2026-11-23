# Etapa 1: compilar con Maven (no hace falta tener Maven instalado en el servidor)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml lombok.config ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen liviana solo con el JRE y el .jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ms-farmacia.jar app.jar
EXPOSE 8083
# Limita la memoria de la JVM al 75 % de lo que Docker le asigne al contenedor
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
