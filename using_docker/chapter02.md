# Chapter 2. Installation

There's a script to install the latest version of Docker on the official site:
```
$ curl https://get.docker.com > /tmp/install.sh
$ cat /tmp/install.sh
...
$ chmod +x /tmp/install.sh
$ /tmp/install.sh
```

But it's fine to use any common installation suite. E.g. on mac os:
```
$ brew install docker docker-compose docker-machine
```

You can check whether it's installed and running correctly this way:
```
$ docker version
Client: Docker Engine - Community
 Version:           19.03.2
 API version:       1.40
 Go version:        go1.12.8
 Git commit:        6a30dfc
 Built:             Thu Aug 29 05:26:49 2019
 OS/Arch:           darwin/amd64
 Experimental:      false

Server: Docker Engine - Community
 Engine:
  Version:          19.03.3
  API version:      1.40 (minimum version 1.12)
  Go version:       go1.12.10
  Git commit:       a872fc2f86
  Built:            Tue Oct  8 01:01:20 2019
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          v1.2.10
  GitCommit:        b34a5c8af56e510852c35414db4c1f4fa6172339
 runc:
  Version:          1.0.0-rc8+dev
  GitCommit:        3e425f80a8c931f88e6d94a8c831b9d5aa481657
 docker-init:
  Version:          0.18.0
  GitCommit:        fec3683
```

If the output is similar - everything's fine. Otherwise try googling :) E.g. here's [the instruction on stackoverflow](https://stackoverflow.com/questions/35969414/couldnt-connect-to-docker-daemon-on-mac-os-x)

When the virtual machine is installed and configured you can run it and stop it this way:
To stop the virtual machine run:
```
$ docker-machine start
...
$ docker-machine stop
```

You can also install and run desktop version of Docker on Mac OS.

