ARG COMPONENT_HTML
ARG COMPONENT_SERVER

FROM $COMPONENT_HTML as html
FROM $COMPONENT_SERVER as server
FROM openjdk:17-jdk-slim

RUN apt-get update && \
    apt-get install iproute2 nfs-common curl gnupg unzip lsof -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*

RUN bash -c 'distribution=$(. /etc/os-release;echo $ID$VERSION_ID) &&\
    curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | apt-key add - && \
    curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | tee /etc/apt/sources.list.d/nvidia-docker.list' && \
    apt-get update && \
    apt-get install -y nfs-common nvidia-container-toolkit netbase && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* && \
    echo 'NEED_STATD=yes' >> /etc/default/nfs-common


COPY entry.sh /usr/bin/
RUN chmod +x /usr/bin/entry.sh


COPY --from=html /var/www/html /var/www/html
COPY --from=server /opt/winslow/winslow.jar /usr/bin/

ENV WINSLOW_STATIC_HTML=/var/www/html/

ENTRYPOINT /usr/bin/entry.sh

