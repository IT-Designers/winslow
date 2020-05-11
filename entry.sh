#!/bin/bash
set -e
trap 'kill -TERM $(pgrep nfs); sleep 2; kill -KILL $(pgrep nfs)' TERM
trap 'kill -KILL $(pgrep nfs)' KILL

export NOMAD_PID_FILE=/run/nomad.pid

echo "  :::: Starting nomad"
#start-stop-daemon --start --name nomad --quiet --pidfile $NOMAD_PID_FILE --background --exec /usr/bin/nomad -- agent -config /etc/nomad/nomad.hcl
start-stop-daemon --start --name nomad --quiet --pidfile $NOMAD_PID_FILE --background --startas /bin/bash -- -c "/usr/bin/nomad agent -config /etc/nomad/nomad.hcl &> /var/log/nomad.log"
sleep 8

echo "  :::: Preparing Winslow startup env"
mkdir -p "$WINSLOW_WORK_DIRECTORY"

if [ "$WINSLOW_STORAGE_TYPE" == "nfs" ]; then
    echo "    :: Preparing NFS Storage"
    mkdir -p /run/sendsigs.omit.d/
    service rpcbind start
#    mount.nfs -o vers=4,intr,soft "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
    mount.nfs -o intr,soft,sync "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"

elif [ "$WINSLOW_STORAGE_TYPE" -ne "" ]; then
    echo "Error: nknown Storage Type: $WINSLOW_STORAGE_TYPE"
    exit 1
fi
    

echo "  :::: Starting winslow"
echo ""
java -jar /usr/bin/winslow.jar

start-stop-daemon --stop --name nomad --quiet --pidfile $NOMAD_PID_FILE

