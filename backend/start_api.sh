sh bin/sbt "kitchen-server/run rest/src/main/resources/docker.config --init-data" &
sh bin/sbt "kitchen-server/run rest/src/main/resources/docker.config" 
