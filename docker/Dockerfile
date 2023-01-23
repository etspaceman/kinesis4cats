FROM azul/zulu-openjdk-alpine:11

ARG DOCKER_SERVICE_JAR
COPY ${DOCKER_SERVICE_JAR} ./${DOCKER_SERVICE_JAR}
RUN apk --no-cache add curl

EXPOSE 4567 4568
CMD ["java", "-jar", "${DOCKER_SERVICE_JAR}"]
