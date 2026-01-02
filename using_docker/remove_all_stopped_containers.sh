docker rm -v $(docker ps -aq -f status=exited)
