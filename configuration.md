![under_construction.png](images/under_construction.png)
# Configuration 
## More Information
### Environment Variables
#### required
* `WINSLOW_WORK_DIRECTORY` Absolut path to the working directory that has to be on a nfs
    * Example `/winslow/workdirectory/that/is/on/nfs`
#### optional
* `WINSLOW_NO_STAGE_EXECUTION` stage execution, act as observer / web-accessor
    * `Values`
        * `1` disable
        * `0` enable stage execution, act as observer / web-accessor

* `WINSLOW_NO_WEB_API` REST/WebSocket-API (no longer starts Spring Boot)
    * `Values`
        * `1` disable
        * `0` enables
 
* `WINSLOW_ROOT_USERS` users with root access
 
##### Development
* `WINSLOW_DEV_ENV` auth and allows root access to all resources
    * `Values`
        * `true` disables
        * `false` enables

* `WINSLOW_DEV_REMOTE_USER` username to assign to (unauthorized) requests


* `WINSLOW_DEV_ENV_IP` publicly visible IP of the WEB-UI
    * Example:  192.168.1.178
 
##### Nvidia GPU (optional)
* `WINSLOW_NO_GPU_USAGE` access to GPUs
    * `Values`
        * `0` disables
        * `1` enables

#### NFS-Server WHAT ABOUT THIS?
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
