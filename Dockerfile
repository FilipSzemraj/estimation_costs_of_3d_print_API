FROM openjdk:21-oracle AS build

WORKDIR /app

COPY mvnw .

COPY .mvn .mvn

COPY pom.xml .

RUN ls -al  # List all files to verify that pom.xml and mvnw are copied

RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ls -al src  # List all files in the src directory to ensure the source code is copied


RUN ./mvnw package -DskipTests -X



RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM openjdk:21-oracle

ARG DEPENDENCY=/app/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java", "-cp", "app:app/lib/*", "com.fsz._3Dcostestimator.Application"]

