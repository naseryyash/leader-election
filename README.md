# Distributed Leader Election Algorithm

This is a very simple, but highly effective distributed leader election algorithm implemented in Java.
[Apache Zookeeper](https://zookeeper.apache.org/) is used for distributed co-ordination amongst nodes.

It is nowhere near as complex as Raft or Paxos or any such distributed consensus algorithms out there.
The purpose is to provide a simple way to elect a leader node amongst nodes running the same process, 
who are equal in compute & memory. Also, included are several fault tolerance mechanisms so that the 
cluster always has a leader and that other nodes know the leader as long as there is atleast a single
node in the cluster.

## Basic algorithm

The following are the basic steps in the algorithm implemented here.

1. All nodes on startup, try to connect to zookeeper and volunteer for leadership.
2. Whichever node manages to register itself first gets to become the leader.
3. All other nodes can see the node registry in zookeeper and know who the leader is immediately.

## Fault tolerance

In case any of the nodes, including the leader, go down, the cluster has to be still functional. 
In the sense that it has always got to have leader node. So, for that purpose, two additions have 
been made to the algorithm.

### Leader re-election

1. If the leader node goes down, that event is detected, and a new leader is automatically elected.
2. If the leader comes back up again, it joins the cluster as a normal node and is immediately 
notified of who the new leader is.

**Notes**: Here the implementation is such that, only the node watching the leader is notified of the leader
going down. This is done to reduce herding effect, i.e. if everyone kept watching the leader, every node in the 
cluster would need to be notified of this event, and that might simply create load on the cluster 
or worse, take it down entirely. But any node can figure out who the new leader is pretty quickly.

### Recovery - node(s) going down.

1. Each node watches a node previously registered in zookeeper for events. (See the reason above in the 
[Leader re-election](#leader-re-election) notes section.)
2. If the node it is watching goes down, it simply watches the node before that.
3. If the node it is watching is the leader, it becomes the leader. (this is essentially [Leader re-election](#leader-re-election))

## Installation and running

### Software Versions 

This project has been tested using the following. But it should reasonably work with any newer versions.
The only noticeable changes can be in the zookeeper APIs, which might not be entirely backward compatible.
Please verify those before running the project with any other version of zookeeper.

1. Java - Oracle JDK 17.0.12
2. Apache Maven - 3.9.8
3. Apache Zookeeper - 3.8.4

### Running instructions

**Zookeeper**

1. Open any cmdline like terminal or gitbash.
2. Navigate to apache-zookeeper-3.8.4-bin\bin, folder in your zookeeper installation.
3. Start the zookeeper server with zkServer.cmd or ./zkServer.sh start (depending on your OS)
4. Verify that zookeeper is up and running on localhost:2181
5. Open up the zookeeper cmdline, (zkCli.cmd for windows or zkCli.sh for linux or MacOS).
6. Create a new node called "/election" (Refer zookeeper documentation for the latest cmd to do so.).

**Project**

1. Open a new terminal/cmdline.
2. Git clone the project.
3. Navigate to the root directory (having pom.xml).
4. Run the following maven command to build the project.
```
mvn clean package
```
5. Open many new terminals/cmdlines (simulating different nodes.)
6. Navigate to the target folder of each terminal/cmdline.
7. Start the processes with the following command.
```
java -jar leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Notes** To simulate faults in the cluster, you can end any running processes with "cmd + c" or "ctrl + c" 
and bring it up again with the above java -jar command to see that the fault tolerance mechanisms work as 
per the spec.
