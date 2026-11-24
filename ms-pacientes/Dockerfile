# Etapa 1: compilar con Maven (no hace falta tener Maven instalado en el servidor)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml lombok.config ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests
# Agente de JaCoCo: mide la cobertura del codigo mientras el servicio atiende peticiones reales
RUN mvn -B -q dependency:copy -Dartifact=org.jacoco:org.jacoco.agent:0.8.13:jar:runtime -DoutputDirectory=/jacoco \
    && mv /jacoco/org.jacoco.agent-0.8.13-runtime.jar /jacoco/jacocoagent.jar

# Etapa 2: imagen liviana solo con el JRE y el .jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ms-pacientes.jar app.jar
# Agente de cobertura y clases compiladas (las usa el reporte de cobertura sobre lo desplegado).
# El agente solo se activa si JAVA_TOOL_OPTIONS lo pide (en AWS, desde docker-compose.aws.yml).
COPY --from=build /jacoco/jacocoagent.jar /opt/jacoco/jacocoagent.jar
COPY --from=build /app/target/classes /opt/app/classes
EXPOSE 8081
# Limita la memoria de la JVM al 75 % de lo que Docker le asigne al contenedor
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
