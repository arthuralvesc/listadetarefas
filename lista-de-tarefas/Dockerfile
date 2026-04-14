FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para baixar as dependências (cache do Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Agora copia o código fonte e gera o .jar
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Run (Onde a aplicação realmente roda)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiamos apenas o .jar gerado no estágio anterior (imagem final fica leve)
COPY --from=build /app/target/*.jar app.jar

# Expondo a porta (informativo)
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]