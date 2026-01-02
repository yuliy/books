# Chapter 3. First Steps

## Hello World
How to check that everything is configured correctly:
```
# start the VM
$ docker-machine start

# configure the shell (I'm not sure whether it's necessary)
$ eval $(docker-machine env)

# Run (and download if not yet downloaded) the "debian" image
docker run debian echo "Hello World"
```

We can ask Docker to give us a shell inside a container with the following command (it's kinda ssh):
```
docker run -i -t debian /bin/bash
```

## The Basic Commands
```
# List all running containers (shows only running containers by default)
$ docker ps

# List all container (both running and stopped)
$ docker ps -a

# Show container info
$ docker inspect <name>

# Show changes made to container
$ docker diff <name>

# Show all commands executed in container
$ docker logs <name>

# Stop running container
$ docker stop <name>

# Remove container
$ docker rm <name>

# How to get rid of all stopped containers
# Here $(docker ps -aq -f status=exited) returns IDs of all stopped containers.
$ docker rm -v $(docker ps -aq -f status=exited)
```

## Creating a new docker image
Here's a simple example
```
# Create a new container
$ docker run -it --name cowsay --hostname cowsay debian bash

# Inside the container call:
root@cowsay:/# apt-get update
root@cowsay:/# apt-get install -y cowsay fortune
root@cowsay:/# /usr/games/fortune | /usr/games/cowsay

# Now we can create an image of this container.
# It doesn't matter if we exit the running container or not.
docker commit cowsay test/cowsayimage

# Now we can try creating a container from our new image:
docker run test/cowsayimage /user/games/cowsay "Moo"
```

## Building Images from Dockerfiles
