FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN ./mvnw clean package -DskipTests

EXPOSE 8182

CMD ["sh", "-c", "java -jar target/*.jar"]