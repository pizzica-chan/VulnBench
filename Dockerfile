# ビルド段階: Maven で fat JAR を生成
FROM public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# 実行段階: JRE のみで起動
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/secapp.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
