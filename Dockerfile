# 1단계: Maven 빌드 단계
FROM maven:3.9.6-eclipse-temurin-11 AS build

# 작업 디렉토리 생성
WORKDIR /app

# pom.xml과 소스 복사
COPY pom.xml .
COPY src ./src

# JAR 빌드 (테스트는 생략)
RUN mvn clean package -DskipTests

# 2단계: 실행 환경 (경량화된 이미지 사용)
FROM eclipse-temurin:11-jre

# 작업 디렉토리 생성
WORKDIR /app

# 빌드된 JAR 복사 (실제 이름은 artifactId-version.jar)
COPY --from=build /app/target/web-login-fetcher-0.0.1-SNAPSHOT.jar app.jar

# 실행 명령
CMD ["java", "-jar", "app.jar"]
