#!/bin/bash
set -e
trap 'kill -TERM $(pgrep nfs); sleep 2; kill -KILL $(pgrep nfs)' TERM
trap 'kill -KILL $(pgrep nfs)' KILL

export NOMAD_PID_FILE=/run/nomad.pid

echo "  :::: Starting nomad"
#start-stop-daemon --start --name nomad --quiet --pidfile $NOMAD_PID_FILE --background --exec /usr/bin/nomad -- agent -config /etc/nomad/nomad.hcl
start-stop-daemon --start --name nomad --quiet --pidfile $NOMAD_PID_FILE --background --startas /bin/bash -- -c "/usr/bin/nomad agent -config /etc/nomad/nomad.hcl &> /var/log/nomad.log"
sleep 5

echo "  :::: Preparing Winslow startup env"
mkdir -p "$WINSLOW_WORK_DIRECTORY"

if [ "$WINSLOW_STORAGE_TYPE" == "nfs" ]; then
    echo "    :: Preparing NFS Storage"
    mount.nfs "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"

elif [ "$WINSLOW_STORAGE_TYPE" -ne "" ]; then
    echo "Error: nknown Storage Type: $WINSLOW_STORAGE_TYPE"
    exit 1
fi
    

echo "  :::: Starting winslow"
echo ""
java -jar /usr/bin/winslow.jar

start-stop-daemon --stop --name nomad --quiet --pidfile $NOMAD_PID_FILE

