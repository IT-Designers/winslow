# How to start the image
```docker run -it --rm -e WINSLOW_DEV_ENV=true -e WINSLOW_DEV_REMOTE_USER=example -e WINSLOW_ROOT_USERS=example -e WINSLOW_WORK_DIRECTORY=/tmp/workdir -p 8080:8080 -v /var/run/docker.sock:/var/run/docker.sock docker.io/itdesigners1/winslow:latest```
