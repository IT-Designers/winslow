![under_construction.png](images/under_construction.png)

# Use Docker with nvidia GPU
## Installation
> [!IMPORTANT]
> Make sure you have a Nvidia GPU and installed the driver.

1. To run docker with nvidia gpu support, you need to install the [nvidia container toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html#installing-with-apt)
1. Configure docker to use the nvidia runtime, see [nvidia documentation](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html#configuring-docker)
   (rootless mode not required)
1. Test the installation with 
   *  `sudo docker run --rm --runtime=nvidia --gpus all ubuntu nvidia-smi`

## Usage
> [!IMPORTANT]
> To use the nvidia runtime, with `Ẁinslow` see [environment variables](configuration#nvidia-gpu-optional) 

## Troubleshooting
If you encounter some problems, you can check the following:
* To check if the nvidia runtime is correctly configured, run 
  * `docker info | grep -i runtimes`
  * you should see something like
    ```
    Runtimes: nvidia runc
    Default Runtime: runc
    ```
* Or see the configuration file
  * `cat /etc/docker/daeon.json`
    ```
    ...
    {
        "runtimes": {
            "nvidia": {
            "args": [],
            "path": "nvidia-container-runtime"
            }
        }
    } 
    ...
    ```
