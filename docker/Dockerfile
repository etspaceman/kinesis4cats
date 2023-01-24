FROM azul/zulu-openjdk-alpine:17

ARG DOCKER_SERVICE_JAR
COPY ${DOCKER_SERVICE_JAR} ./${DOCKER_SERVICE_JAR}
RUN apk --no-cache add curl

EXPOSE 8080
CMD ["java", "-jar", "${DOCKER_SERVICE_JAR}"]
