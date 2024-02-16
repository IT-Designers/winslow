# Winslow Docker Image
## Run the Winslow Docker container

### Start the Docker container:
```bash
docker run -it --rm -e WINSLOW_DEV_ENV=true -e WINSLOW_DEV_REMOTE_USER=example -e WINSLOW_ROOT_USERS=example -e WINSLOW_WORK_DIRECTORY=/tmp/workdir -p 8080:8080 -v /var/run/docker.sock:/var/run/docker.sock docker.io/itdesigners1/winslow:latest
```

- `WINSLOW_DEV_ENV=true` - Set the environment to development
- `WINSLOW_DEV_REMOTE_USER=example` - Set the remote user
- `WINSLOW_ROOT_USERS=example` - Set the root user
- `WINSLOW_WORK_DIRECTORY=/tmp/workdir` - Set the work directory
- `-p 8080:8080` - Expose the port 8080
- `-v /var/run/docker.sock:/var/run/docker.sock` - Mount the docker socket

Try to access the Winslow UI at [http://localhost:8080](http://localhost:8080) with your browser. <br>
Credentials are example/example.

Further documentation can be found on [GitHub](https://github.com/IT-Designers/winslow/tree/2024.1).

## Tags
There are following tags available:
- `20xx.x` - The release version of Winslow, which matches the corresponding git **tag** `20xx.x`
- `20xx.x-dev` - The current state of the corresponding git **branch** `20xx.x`
- `dev` - The current state of git master branch
