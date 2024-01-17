# Winslow 

## Prerequisite
This Project is designed to be used for a Linux Environment.
:warning It is not tested for a Windows Environment. :warning

Software dependencies:
* Docker
* Java 17
* Maven TBD
* Angular 17
* node 18
* npm 9.x

## Run locally
### How to Start
1. Checkout the master branch:
   * `git clone <insert-url>`
1. Create a workdir, for example in the project root
    * `cd winslow && mkdir workdir`
1. Build & Start the Frontend
   * `(cd ui-ng && npm install && npm build && npm run start)`
1. Build the Backend
   * `mvn package`
1. Set environment variables
   * 
     ```
     export WINSLOW_DEV_ENV=true
     export WINSLOW_DEV_ENV_IP=192.168.1.178
     export WINSLOW_NO_GPU_USAGE=0
     export WINSLOW_NO_STAGE_EXECUTION=0
     export WINSLOW_WORK_DIRECTORY=<absolut-path-to-workdir-from-step-2>
     export WINSLOW_DEV_REMOTE_USER=<local>
     export WINSLOW_ROOT_USERS=<local>
     ```
1. Start the Backend
     * `(cd application/target && java -jar winslow-application*.jar)`

---
## Setup Local Development


---
Winslow requires a reachable nomad instance on localhost and the work directory to be a NFS-mount (`/etc/fstab` is parsed to determine the NFS-Server-Path if `WINSLOW_STORAGE_TYPE` and `WINSLOW_STORAGE_PATH` is not set).

#### Configuration
To configure Winslow, Environment variables are used (which also allows the docker image of Winslow to be configure in the same way). The following subset is useful for the local dev environment (see [`de.itdesigners.winslow.Env`](application/src/main/java/de/itdesigners/winslow/Env.java) for the complete list):

```bash
# required
WINSLOW_WORK_DIRECTORY=/winslow/workdirectory/that/is/on/nfs

# optional
WINSLOW_NO_STAGE_EXECUTION=1 # disable stage execution, act as observer / web-accessor
WINSLOW_DEV_ENV=true # disables auth and allows root access to all resources
WINSLOW_DEV_REMOTE_USER=mi7wa6 # username to assign to (unauthorized) requests
WINSLOW_DEV_ENV_IP=192.168.1.178 # publicly visible IP of the WEB-UI
WINSLOW_NO_GPU_USAGE=0 # disable access to GPUs
WINSLOW_NO_WEB_API=1 # disable REST/WebSocket-API (no longer starts Spring Boot)
WINSLOW_ROOT_USERS=mi7wa6 # users with root access

# ask IT for credentials
WINSLOW_LDAP_MANAGER_PASSWORD=... 
WINSLOW_LDAP_MANAGER_DN=cn=...,dc=...,dc=...
WINSLOW_LDAP_URL=ldaps://ldap1.../ ldaps://ldap2.../
WINSLOW_LDAP_GROUP_SEARCH_BASE=ou=Groups,ou=...,dc=...,dc=...
WINSLOW_LDAP_USER_SEARCH_BASE=ou=Users,ou=...,dc=...,dc=...
```

These environment variables can be set in the `Run/Debug Configuration` in IntelliJ.

#### Nomad

Download nomad via https://www.nomadproject.io/downloads and run it with
```bash
nomad agent --config nomad.hcl
```

An up to date version of the configuration can be found [here](../../../../docker/-/blob/master/nomad.hcl):
```hcl
datacenter = "local"
data_dir = "/tmp/nomad-data-dir"

server {
  enabled = true
  bootstrap_expect = 1
}

client {
  enabled = true
}

plugin "docker" {
  config {
    allow_privileged = true

    gc {
      image_delay = "96h"
    }

    volumes {
      enabled = true
    }
  
  }
}
```

#### NFS-Server

Install `nfs-kernel-server`: `sudo apt install nfs-kernel-server` and update `/etc/export`:

```nfs
/path/to/nfs-export *(rw,no_root_squash,all_squash,fsid=1,anonuid=0,anongid=0) 172.0.0.0/8(rw,no_root_squash,all_squash,fsid=1,anonuid=0,anongid=0)
/path/to/nfs-export/run *(rw,no_root_squash,all_squash,fsid=2,anonuid=0,anongid=0) 172.0.0.0/8(rw,no_root_squash,all_squash,fsid=2,anonuid=0,anongid=0)

```


Add to `/etc/fstab` an entry to mount the nfs directory

```fstab
<your-pc-name>:/path/to/nfs-export /home/<username>/path/to/nfs-mount nfs noauto 0 0

# winslow/run store very small temporary files, making it a tmpfs makes it faster (ram-fs)
tmpfs /path/to/nfs-export/run tmpfs size=1G,mode=760,noauto 0 0
```


Run the following script (`./start-nfs-server.sh`):

```bash
#!/bin/bash

sudo mount nfs-export/run
sudo service nfs-kernel-server restart
sleep 5
sudo mount nfs-mount
```
