datacenter = "local"
data_dir = "/tmp/nomad-data-dir"

server {
  enabled = true
  bootstrap_expect = 1
}

client {
  enabled = true
}
