#!/bin/bash

#IMAGE="winslow"
IMAGE="repo.itd-intern.de/winslow/node"
HTTP="80"
PARAMS="$@"
STORAGE_TYPE="nfs"
STORAGE_PATH="pc973l:/home/mi7wa6/mec-view/winslow/nfs-export"
WORKDIR="/winslow/"

NODE_NAME="$(hostname)"
ADDITIONAL="-p 446:4646 -e WINSLOW_DEV_ENV=true -e WINSLOW_DEV_REMOTE_USER=michael"
GPUS="$(ls /dev/ | grep -i nvidia | wc -l)"
SUDO=""

if [ "$(id -u)" -ne 0 ] && [ "$(id --name -G | grep -i docker | wc -l)" -eq 0 ]; then
    SUDO="sudo"
fi

$SUDO docker pull $IMAGE


echo ""
echo ""
echo ""
echo " :::::  Going to create Winslow Container with the following settings"
echo ""
echo "   HTTP Port    '$HTTP'"
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

GPU_FLAG=""

if [ "$GPUS" -gt 0 ]; then
    GPU_FLAG="--gpus all"
fi

echo " ::::: Starting Winslow Container now"
$SUDO docker run -it --rm --privileged \
     $USE_ALL_GPUS \
    -p $HTTP:8080 \
    $ADDITIONAL \
    -e WINSLOW_STORAGE_TYPE=$STORAGE_TYPE \
    -e WINSLOW_STORAGE_PATH=$STORAGE_PATH \
    -e WINSLOW_WORK_DIRECTORY=$WORKDIR \
    -e "WINSLOW_NODE_NAME=$NODE_NAME" \
    -v /var/run/docker.sock:/var/run/docker.sock \
    $IMAGE \
    $PARAMS
