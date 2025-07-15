FROM azul/zulu-openjdk-alpine:21

ARG DOCKER_SERVICE_JAR
COPY ${DOCKER_SERVICE_JAR} ./service.jar

RUN apk --no-cache add curl

EXPOSE 8080
CMD ["sh", "-c", "java -jar service.jar"]
