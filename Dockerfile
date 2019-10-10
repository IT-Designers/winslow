FROM repo.itd-intern.de/winslow/component-server as server
FROM repo.itd-intern.de/winslow/component-html as html

FROM openjdk:11-jre-slim-buster

RUN apt update && \
    apt install nginx iproute2 nfs-common curl gnupg -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*

RUN bash -c 'distribution=$(. /etc/os-release;echo $ID$VERSION_ID) &&\
    curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | apt-key add - && \
    curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | tee /etc/apt/sources.list.d/nvidia-docker.list' && \
    apt-get update && \
    apt-get install -y nvidia-container-toolkit && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*

COPY entry.sh /usr/bin/
RUN chmod +x /usr/bin/entry.sh

COPY nomad /usr/bin/
COPY nomad.hcl /etc/nomad/nomad.hcl

COPY --from html /etc/nginx/sites-available/default /etc/nginx/sites-available/default
COPY --from html /var/www/html /usr/share/nginx/html
COPY --from server /opt/winslow/winslow.jar /usr/bin/

ENTRYPOINT /usr/bin/entry.sh

