FROM openjdk:17

ADD winslow-application*.jar /opt/winslow/winslow.jar

WORKDIR /opt/winslow
ENTRYPOINT java -jar winslow.jar
