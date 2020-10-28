# Winslow 

### Run local

Winslow requires a running nomad instance on localhost and the work directory to be a NFS-mount (```/etc/fstab``` is parsed to determine the NFS-Server-Path).

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

# Ask IT for credentials
WINSLOW_LDAP_MANAGER_PASSWORD=... 
WINSLOW_LDAP_MANAGER_DN=cn=...,dc=...,dc=...
WINSLOW_LDAP_URL=ldaps://ldap1.../ ldaps://ldap2.../
WINSLOW_LDAP_GROUP_SEARCH_BASE=ou=Groups,ou=...,dc=...,dc=...
WINSLOW_LDAP_USER_SEARCH_BASE=ou=Users,ou=...,dc=...,dc=...
```