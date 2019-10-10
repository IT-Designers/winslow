FROM openjdk:11

ADD target/winslow*.jar /opt/winslow/winslow.jar

WORKDIR /opt/winslow
ENTRYPOINT java -jar winslow.jar
