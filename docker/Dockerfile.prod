FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0 AS assembly
WORKDIR /build
COPY ./project/plugins.sbt ./project/plugins.sbt
COPY ./configs ./configs
COPY ./build.sbt ./build.sbt
COPY ./src ./src
RUN sbt "clean" "test" "assembly"

FROM eclipse-temurin:17.0.4.1_1-jre-jammy
ENV JAR_FILE="target/scala-3.2.0/assembly.jar"
RUN useradd non-root-user
USER non-root-user
WORKDIR /app
COPY --from=assembly "/build/$JAR_FILE" "./$JAR_FILE"
ENTRYPOINT [ "/bin/sh", "-c", "java -jar /app/$JAR_FILE" ]
