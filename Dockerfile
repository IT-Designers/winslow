FROM openjdk:11

ADD target/winslow-1.0-SNAPSHOT.jar /opt/winslow/winslow.jar

WORKDIR /opt/winslow
ENTRYPOINT java -jar winslow.jar
