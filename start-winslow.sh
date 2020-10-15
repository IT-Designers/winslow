#!/bin/bash

#HTTP="80"
#HTTPS="443"
#NODE_TYPE="executor"
NODE_TYPE="observer"
STORAGE_TYPE="nfs"
STORAGE_PATH="srv515.itd-intern.de:/data/streets/winslow"
#KEYSTORE_PATH_PKCS12="/root/winslow.itd-intern.de.p12"
ADDITIONAL="-p 446:4646 -e WINSLOW_DEV_ENV=true -e WINSLOW_DEV_REMOTE_USER=michael"

PARAMS="$@"
#IMAGE="winslow"
IMAGE="repo.itd-intern.de/winslow/node"
NODE_NAME="$(hostname)"
CONTAINER_NAME="winslow"
GPUS="$(ls /dev/ | grep -i nvidia | wc -l)"
WORKDIR="/winslow/"

SUDO=""

if [ "$(id -u)" -ne 0 ] && [ "$(id --name -G | grep -i docker | wc -l)" -eq 0 ]; then
    SUDO="sudo"
fi

$SUDO docker pull $IMAGE

if [ "$KEYSTORE_PATH_PKCS12" != "" ]; then
    ADDITIONAL="$ADDITIONAL -v $KEYSTORE_PATH_PKCS12:/keystore.p12:ro -e SERVER_SSL_KEY_STORE_TYPE=PKCS12 -e SERVER_SSL_KEY_STORE=file:/keystore.p12 -e SECURITY_REQUIRE_SSL=true -e SERVER_SSL_KEY_STORE_PASSWORD="
fi

if [ "$HTTP" == "" ] && [ "$HTTPS" == "" ]; then
    ADDITIONAL+=" WINSLOW_NO_WEB_API=true "
fi


echo ""
echo ""
echo ""
echo " :::::  Going to create Winslow Container with the following settings"
echo ""
echo "   HTTP Port    '$HTTP'"
echo "   HTTPS Port   '$HTTPS'"
echo "   Docker Image '$IMAGE'"
echo "   Storage Type '$STORAGE_TYPE' @ '$STORAGE_PATH'"
echo ""
echo "   Work Directory '$WORKDIR'"
echo "   Node Name      '$NODE_NAME'"
echo ""
echo "   Detected GPUs: $GPUS"
echo ""
echo "   Additional Docker Parameters:  '$ADDITIONAL'"
echo "   Additional Winslow Parameters: '$PARAMS'"
echo ""

sleep 1

if [ $($SUDO docker ps --filter "name=$CONTAINER_NAME" | wc -l) -gt 1 ]; then
    echo " ::::: Stopping already running Winslow instance"
    $SUDO docker stop "$CONTAINER_NAME" > /dev/null && echo "  :::: Done" || (echo " :::: Failed"; exit 1)
fi


echo " ::::: Starting Winslow Container now"
$SUDO docker run -it --rm --privileged \
    --name "$CONTAINER_NAME" \
    $(if [ "$GPUS" -gt 0 ]; then echo "--gpus all"; fi) \
    $(if [ "$HTTP" != "" ] ; then echo " -p $HTTP:8080"; fi) \
    $(if [ "$HTTPS" != "" ] ; then echo " -p $HTTPS:8080"; fi) \
    $ADDITIONAL \
    -e WINSLOW_STORAGE_TYPE=$STORAGE_TYPE \
    -e WINSLOW_STORAGE_PATH=$STORAGE_PATH \
    -e WINSLOW_WORK_DIRECTORY=$WORKDIR \
    -e "WINSLOW_NODE_NAME=$NODE_NAME" \
    $(if [ "$NODE_TYPE" == "observer" ]; then echo "-e WINSLOW_NO_STAGE_EXECUTION=1"; fi) \
    -v /var/run/docker.sock:/var/run/docker.sock \
    $IMAGE \
    $PARAMS
