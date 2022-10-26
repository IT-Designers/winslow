FROM nexus.itd-intern.de/winslow/component-html as html
FROM nexus.itd-intern.de/winslow/component-server as server

RUN apt update && \
    apt install iproute2 nfs-common curl gnupg unzip lsof -y && \
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

RUN curl -o nomad.zip https://releases.hashicorp.com/nomad/1.1.2/nomad_1.1.2_linux_amd64.zip && \
    unzip nomad.zip && \
    rm nomad.zip && \
    mv nomad /usr/bin/nomad


COPY entry.sh /usr/bin/
RUN chmod +x /usr/bin/entry.sh

#COPY nomad /usr/bin/
COPY nomad.hcl /etc/nomad/nomad.hcl

COPY --from=html /var/www/html /var/www/html
COPY --from=server /opt/winslow/winslow.jar /usr/bin/

ENV WINSLOW_STATIC_HTML=/var/www/html/

ENTRYPOINT /usr/bin/entry.sh

