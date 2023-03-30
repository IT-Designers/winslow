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
