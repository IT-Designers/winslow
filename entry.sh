#!/bin/bash
set -e
trap 'kill -TERM $(pgrep nfs); sleep 2; kill -KILL $(pgrep nfs)' TERM
trap 'kill -KILL $(pgrep nfs)' KILL

KEYSTORE="$JAVA_HOME/lib/security/cacerts"

if [ "$WINSLOW_CA_CERT_DIR" != "" ]; then 
  (update-ca-certificates)
  IFS_OLD=$IFS
  IFS=$'\n'
  for f in $(find "$WINSLOW_CA_CERT_DIR" -type f); do
    echo "Importing $f"
    (keytool -delete -trustcacerts -keystore -cacerts -storepass changeit -noprompt -alias "$f" || true)
    (keytool -import -trustcacerts -keystore -cacerts -storepass changeit -noprompt -alias "$f" -file "$f")
  done
  IFS=$IFS_OLD
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
    #     https://unix.stackexchange.com/questions/149029/pernicious-usb-stick-stall-problem-reverting-workaround-fix/149140
    # but dont do this on the nfs-server
    #     https://www.suse.com/support/kb/doc/?id=000017857
    if [ "$WINSLOW_NO_DIRTY_BYTES_ADJUSTMENT" == "" ] ; then
        echo " ::::: Adjusting Dirty-Bytes for NFS Storage"
        check_set_val "/proc/sys/vm/dirty_bytes" $((64*1024*1024))
        check_set_val "/proc/sys/vm/dirty_background_bytes" $((8*1024*1024))
    else
        echo " ::::: Skipping Dirty-Bytes adjustment"
    fi

    mkdir -p /run/sendsigs.omit.d/
    service rpcbind start
#    mount.nfs -o vers=4,intr,soft "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft,sync "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft,async "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
#    mount.nfs -o intr,soft,async,lookupcache=none "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
    COMMON_NFS_OPTIONS=noatime,nodiratime,soft
    mount.nfs -o $COMMON_NFS_OPTIONS,async "$WINSLOW_STORAGE_PATH" "$WINSLOW_WORK_DIRECTORY"
    # lookupcache=none to immidiately detect new directory entries (event files)
    # noac             to immidiately detect file changes (to project files)
    mount.nfs -o $COMMON_NFS_OPTIONS,lookupcache=none "$WINSLOW_STORAGE_PATH/run" "$WINSLOW_WORK_DIRECTORY/run"
    mount.nfs -o $COMMON_NFS_OPTIONS,noac             "$WINSLOW_STORAGE_PATH/projects" "$WINSLOW_WORK_DIRECTORY/projects"
    mount.nfs -o $COMMON_NFS_OPTIONS,noac             "$WINSLOW_STORAGE_PATH/pipelines" "$WINSLOW_WORK_DIRECTORY/pipelines"

elif [ "$WINSLOW_STORAGE_TYPE" == "bind" ]; then
    echo "    :: Storage is provided through binding"

elif [ "$WINSLOW_STORAGE_TYPE" != "" ]; then
    echo "Error: unknown storage type: $WINSLOW_STORAGE_TYPE"
    exit 1
fi

ANGENTLIB_DEBUGGER=""
if [ "$WINSLOW_REMOTE_DEBUGGER" != "" ] && [ "$WINSLOW_REMOTE_DEBUGGER" != "0" ]; then
    whereis java
    java -version
    AGENTLIB_DEBUGGER=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6006 "
fi

echo "  :::: Starting winslow"
echo ""
JAVA_CMD="java $AGENTLIB_DEBUGGER -jar /usr/bin/winslow.jar"
echo "$JAVA_CMD"
$JAVA_CMD

start-stop-daemon --stop --name nomad --quiet --pidfile $NOMAD_PID_FILE

