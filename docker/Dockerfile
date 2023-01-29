FROM azul/zulu-openjdk:17

ARG DOCKER_SERVICE_JAR
COPY ${DOCKER_SERVICE_JAR} ./service.jar

RUN apt-get update && apt-get -y -qq install curl

EXPOSE 8080
CMD ["sh", "-c", "java -jar service.jar"]
