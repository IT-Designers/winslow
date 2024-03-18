![under_construction.png](images/under_construction.png)
# Configuration 
## More Information
### Environment Variables
#### required
* `WINSLOW_WORK_DIRECTORY` Absolut path to the working directory that has to be on a nfs
    * `Values`
      * default: `/winslow/`
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
### sort

* WINSLOW_STORAGE_TYPE defines the type
    * `Values`
      * default `bind` used for local storage
      * `nfs` network file storage

* WINSLOW_NODE_NAME
    * `Values`
      * default: name of host from `/etc/hostname`
* WINSLOW_API_PATH defines the root uri for the backend server
    * `Values`
      * `default`: "/api/v1/"

* WINSLOW_WEBSOCKET_PATH 
    * `Values`
      * `default`:  "/ws/v1/"

* WINSLOW_STATIC_HTML
    * `Values`
* WINSLOW_DEV_ENV_IP: defines for development? TODO
    * `Values`
      * Example: 10.20.30.40
* WINSLOW_WEB_REQUIRE_SECURE: activates usage of TLS certificate? TODO
    * `Values`
        * `0` disables
        * `1` enables

* WINSLOW_LOCK_DURATION_MS lock something don't know what TODO
    * `Values`
        * minimum: 10s
      * `default`: 5min
* WINSLOW_LDAP_URL
    * `Values`
        * `0` disables
        * `1` enables
* WINSLOW_ROOT_USERS
    * `Values`
        * `0` disables
        * `1` enables
* WINSLOW_AUTH_METHOD
    * `Values`
        * `ldap`
        * `local`
* WINSLOW_BACKEND
    * `Values`
        * `0` disables
        * `1` enables
* WINSLOW_BACKEND_DOCKER
    * `Values`
        * `0` disables
        * `1` enables

