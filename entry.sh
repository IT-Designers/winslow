#!/bin/bash
set -e
trap 'kill -TERM $(pgrep nfs); sleep 2; kill -KILL $(pgrep nfs)' TERM
trap 'kill -KILL $(pgrep nfs)' KILL

KEYSTORE="$JAVA_HOME/lib/security/cacerts"

if [ "$WINSLOW_CA_CERT_DIR" != "" ]; then 
  update-ca-certificates
  IFS=$'\n'
  for f in $(find "$WINSLOW_CA_CERT_DIR" -type f); do
    echo "Importing $f"
    keytool -delete -trustcacerts -keystore -cacerts -storepass changeit -noprompt -alias "$f" || true
    keytool -import -trustcacerts -keystore -cacerts -storepass changeit -noprompt -alias "$f" -file "$f"
  done
fi


export NOMAD_PORT=4646
export NOMAD_PID_FILE=/run/nomad.pid

echo "  :::: Starting nomad"
rm -rf /tmp/nomad-data-dir || true
#start-stop-daemon --start --name nomad --quiet --pidfile $NOMAD_PID_FILE --background --exec /usr/bin/nomad -- agent -config /etc/nomad/nomad.hcl
start-stop-daemon --start --name nomad --quiet --pidfile $NOMAD_PID_FILE --background --startas /bin/bash -- -c "/usr/bin/nomad agent -config /etc/nomad/nomad.hcl &> /var/log/nomad.log"

for i in {1..15}; do
  sleep 1
  echo "       Probing nomad... ($i)"
  if $(lsof -Pi :$NOMAD_PORT -sTCP:LISTEN > /dev/null); then
    break;
  fi
done

if $(lsof -Pi :$NOMAD_PORT -sTCP:LISTEN > /dev/null); then
  echo "       Probing nomad... succeeded"
else
  echo "       Probing nomad... failed"
  exit 2
fi

echo "  :::: Preparing Winslow startup env"
mkdir -p "$WINSLOW_WORK_DIRECTORY"

if [ "$WINSLOW_STORAGE_TYPE" == "nfs" ]; then
    echo "    :: Preparing NFS Storage"

    function check_set_val() {
        if [ "$(cat $1)" != "$2" ]; then
            echo "Writing $2 to $1"
            echo $2 > $1
        fi
    }

    # Shrink the disk buffers to a more reasonable size. See http://lwn.net/Articles/572911/
    # https://unix.stackexchange.com/questions/149029/pernicious-usb-stick-stall-problem-reverting-workaround-fix/149140
    check_set_val "/proc/sys/vm/dirty_bytes" $((4*1024*1024))
    check_set_val "/proc/sys/vm/dirty_background_bytes" $((4*1024*1024))

    
    mkdir -p /run/sendsigs.omit.d/
    service rpcbind start
#    mount.nfs -o vers=4,intr,soft "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft,sync "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft,async "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
    mount.nfs -o intr,soft,async,lookupcache=none "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"

elif [ "$WINSLOW_STORAGE_TYPE" == "bind" ]; then
    echo "    :: Storage is provided through binding"

elif [ "$WINSLOW_STORAGE_TYPE" != "" ]; then
    echo "Error: unknown storage type: $WINSLOW_STORAGE_TYPE"
    exit 1
fi

ANGENTLIB_DEBUGGER=""
if [ "$WINSLOW_REMOTE_DEBUGGER" != "" ] && [ "$WINSLOW_REMOTE_DEBUGGER" != "0" ]; then
    AGENTLIB_DEBUGGER=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6006 "
fi

echo "  :::: Starting winslow"
echo ""
java $AGENTLIB_DEBUGGER -jar /usr/bin/winslow.jar

start-stop-daemon --stop --name nomad --quiet --pidfile $NOMAD_PID_FILE

