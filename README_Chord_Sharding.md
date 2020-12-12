### CS 441 - Project

### Project Details: To create Chord Algorithm using Typed Akka and Akka-HTTP and Cluster Sharding

#### Project Members: (in Alphabetical Order)
* **Karan Venkatesh Davanam**
* Prajwal Kishor Kammardi
* Rishabh Mehta
* Shabbir Bohra

#### Development Environment
* Development environment: Windows 10
* Frameworks Akka 2.6.10, Akka-HTTP 10.2.1, Akka-Stream 2.6.10,  MoultingYaml 0.4.2, Akka-Cluster-Sharding-typed 2.6.10, Akka-Management 1.0.9
* IDE Used: IntelliJ IDEA 2020.2.3
* Build Tools: SBT


#### Aim
To implement [**Chord: A Scalable Peer-to-peer Lookup Protocol for Internet Applications**](https://pdos.csail.mit.edu/papers/chord:sigcomm01/chord_sigcomm.pdf)

#### Steps to run
1. To run using sbt build tool
    1. Clone the repository [OverlayNetworkSimulator_Group1](https://bitbucket.org/cs441-fall2020/overlaynetworksimulator_group1/)
    `git clone https://prajwalkk@bitbucket.org/cs441-fall2020/overlaynetworksimulator_group1.git`
    Make sure to be on the master branch.
    1. This project has two main classes. Make sure to start the user simulation only after seeing the "Snapshots Captured" message. This means that the nodes are up and running. 
        `sbt runMain com.chord.akkasharding.SimulationDriver`
    1. Run the user simulation only after seeing the above messages, else it will fire requests to a non existing HTTP server
        `sbt runMain com.chord.akkasharding.UserSimulationDriver`
1. To run using docker 
    1. Pull the image from docker hub `docker pull prajwalkk/overlay-sim:1.0` 
    1. Run the docker container in background using `docker run -t -d prajwalkk/overlay-sim:1.0`
    1. Do a `docker ps` and make a note of the ID of the container.
    1. Create 2 docker shell sessions in 2 terminal windows using `docker exec -it "id-of-running-container" bash`
    1. In the first container bash terminal, execute `./runChordNode.sh`
    1. Wait until the above finishes execution (you will see a "Capturing Snapshots" log. Then run `./runChordUser.sh` )
    1. Press Ctrl + C in the session where you ran the `runChordNode.sh` when the user simulations are finished. 
    1. Close the docker terminal and stop the container using `docker stop <container_name>` (Lookup the container name using `docker ps` under the header *NAMES*)

#### Features of the Project
1. The Project is fully written in Scala
1. The Project fully makes use of Akka-Typed Behaviors model. It may seem to look like it is OO model, but the Actors are modelled in such a way that the Objects represents Protocols used by the actor and the classes use the functions as Behaviors. This style of separation of concerns was suggested by the [style guide](https://doc.akka.io/docs/akka/current/typed/style-guide.html)
1. The Project uses Akka cluster sharding to shard the Chord nodes.
1. The Project makes full use of the iterative style of the Chord Algorithm specified in the paper with the link above.
1. The data used by the Project is a collection of dialogues as keys and their Shakespeare-an conversion. They contain spaces and are encoded before firing a request or sending a response. 

#### Architecture
![Architecture](docs/Chord_Shard_Architecture.png)

#### Docker and AWS EC2 Deployment video:
[Project Deployment Video](https://youtu.be/tW1qjYPexvw)

#### Code Flow
* The Project is an attempted Monte Carlo simulation of how a P2P distributed system tends to store the data in the network
It involves the following modules
    * Simulation Driver 
        * Main entry point for Node Creation, HTTP Server and Capturing Yaml snapshots
        * Present in the `com.chord.akkasharding` package
    * User Simulation Driver
        * Entry point for user creation
        * Initializing chord data and generate random-lookup and insert
        * The object that contains function to execute the Monte carlo simulation  
        * Collects and collates results by passing messages to NodeGroup Actor 
        * Present in the `com.chord.akkasharding` package  
    * Simulation object
        * Initializes ClusterSharding and AkkaManagement
        * Creates userGroup, NodeGroup, HTTP server actors 
        * Shards the NodeActor Type
        * Present in `com.chord.akkasharding.simulation` package
    * Node Group. 
        * This module is responsible to hold the information about the node actors in the Akka System.
        * This is an Akka Actor System
        * The node group's job is to Create the number of nodes(entities for the shard region) that is specified in the configuration file. 
        * This captures the Node Actors' **snapshot**. Aggregates them and writes them to a file. 
        * present in the `com.chord.akkasharding.actors` package
    * Node Actor
        * Entities present in each Shard.
        * The main Computing component
        * `com.chord.akkasharding.actors` package
        * Each node implements the functions specified in the chord algorithm
        * A node is added to the ring sequentially.
        * While joining the ring, the node's finger table is initialized. If it is the first node, it's successor and predecessor are set to itself
        * A node stores the data it is responsible for, uses fingertable, hashing and the key to decide on that
        * Node Operations. They are either messages or functions. The decisions of choosing them to be messages and functions are elaborated below  
            1. `Join`
                * This is a Fire and Forget message send my the NodeGroup to the node.
                * If the node is the first node, it sets itself as it's successor and predecessor. Else, it executes below: 
                    * Runs the `InitFingerTable` Function
                    * Tells other nodes via `UpdateOthers` to update their entries 
            2.  `init_finger_table`
                * This is a function that is executed by a new node which is not the first node to join. 
                * It gets its successor by `FindSuccessor`. This is a blocking call that waits for a response before continuing
                * Find Successor gets its value by executing `find_predecessor` function: This was made a function rather than a message as it was creating race conditions when a node asked itself.
                * `ClosestPrecedingFinger` is a message asked by the function in `find_predecessor`. Here if, there is a check that sees if same node is asking itself, if so, the node properties are returned directly rather than wasting time asking and awaiting for reply. Also reduces deadlocks.
                * `SetPredecessor` message is called to set the predecessor of the to-be-succeeding-node to the node that sent the message. This message is made blocking to ensure validity of the finger table in the future runs. 
                * The finger table of the node is updated to reflect the successors for entry.
                * Some other auxiliary messages used in this step are `GetPredecessor` ask call to get the predecessor of a node. 
                * the node is restarted with it's updated values  
            3.  `UpdateOthers` it is a tell message that informs the other nodes to update their Finger table to reflect the entries of the callee via the,
            4.  `UpdateFingerTable` is a tell message that tells the node to update its fingertable
            5. `SendSnapshot` `SendDataSnapshot`. These messages are provided to capture the current state of the system. 
    * User Group 
        * Creates user actors and maintains their addresses. 
        * `com.chord.akkasharding.actors` package
    * User Actor
        * It models a user who can:
            1. `lookup_data` Search for the keys to be inserted 
            2. `put_data` Insert data to the system
    * HTTP Server
        * This orchestrates the message from the user to the node component.
        * Set on `localhost:8000` the path are `/chord`
        * The get request for `lookup_data` is like `http://127.0.0.1:8000/chord/<key>`
        * the post request for `put_data` is like `http://127.0.0.1:8000/chord` and a json payload `{"key":"$key","value":"$value"}`
    * Utils
        The Utils package contains classes to perform operations
            * `DataUtils` read_data() of a CSV file
            * `Helper` Contains hashing functions (SHA1), Range Validation function used in the Chord Algorithm.
            * `MyYaml*Protocol` case classes to convert NodeConfigurations to a YAML (YAML Ain't a Markup Language) file.
            * `YamlDump*` case class to model YAML file. The above two are needed to for the MoultingYaml Package
            
#### Execution Sample 
![Execution Sample](docs/chord_sharding_screenshot.gif)

#### Results
* The server addition logs are as below: 

```
[akka://NodeActorSystem/system/sharding/NodeActor/133/Node_0] - Ref_n : EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeAc
tor), Node_0), Ref n_dash: EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)
[2020-12-10 17:19:18,632] [INFO] [akka://NodeActorSystem/system/sharding/NodeActor/132/Node_1] - Ref_n : EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeAc
tor), Node_1), Ref n_dash: EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)
[2020-12-10 17:19:18,632] [INFO] [akka://NodeActorSystem/system/sharding/NodeActor/133/Node_0] - [Join] First node Node_0 joining: n: EntityRef(EntityTypeKey[com.chord.akkasharding.act
ors.NodeActor$Command](NodeActor), Node_0) with nDash : EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)
[2020-12-10 17:19:18,632] [INFO] [akka://NodeActorSystem/system/sharding/NodeActor/132/Node_1] - [Join] Node_1 node joining
[2020-12-10 17:19:18,634] [INFO] [akka://NodeActorSystem/system/sharding/NodeActor/133/Node_0] - [Join] Node_0 Updated node Properties: node :Node_0
nodeRef :EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)
nodeSuccessor : EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)
nodePredecessor :EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0),
nodeFingerTable:
List(
247 | [247, 248) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
248 | [248, 250) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
250 | [250, 254) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
254 | [254, 6) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
6 | [6, 22) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
22 | [22, 54) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
54 | [54, 118) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)),
118 | [118, 246) | Some(EntityRef(EntityTypeKey[com.chord.akkasharding.actors.NodeActor$Command](NodeActor), Node_0)))

```
* User creation logs are as below:

```log
[2020-12-10 17:49:57,086] [INFO] [akka://UserActorSystem/user] - Creating 3 Users
[2020-12-10 17:49:57,091] [INFO] [akka://UserActorSystem/user] - User Created akka://UserActorSystem/user/User_0
[2020-12-10 17:49:57,091] [INFO] [akka://UserActorSystem/user] - User Created akka://UserActorSystem/user/User_1
[2020-12-10 17:49:57,093] [INFO] [akka://UserActorSystem/user] - User Created akka://UserActorSystem/user/User_2
``` 
* HTTP: Server Creation and data insertion log:

```log
[2020-12-10 17:19:18,570] [INFO] [] - Binding HTTP Server
[2020-12-10 17:19:18,613] [INFO] [] - Trying to start HTTP Server
[2020-12-10 17:19:18,618] [INFO] [] - HTTPServer online at http://127.0.0.1:8000/
```

```log
[2020-12-10 17:50:17,147] [INFO] [] - Inserting 75 records
[2020-12-10 17:50:17,152] [INFO] [] - Initializing Chord data
```

* Http Lookup log

```log
[2020-12-10 18:02:31,373] [INFO] [] - Starting look ups on initialized data
[2020-12-10 18:02:31,373] [INFO] [] - Read Requests started

[2020-12-10 18:02:46,749] [INFO] [akka://NodeActorSystem/system/sharding/NodeActor/109/HTTPServer] - [getValue]  Data found at Node_4 key: No,+not+like+before. ; value : No, indeed are they not.
```
* YAML dump. Signifying the Node state
    *   Single node example for Node YAML
    
```yaml
ArrivaltimeStamp: '2020-12-10T16:07:55.128885700'
NodeProps:
  node_name: Node_3
  nodeRef: Node_3
  timestamp: '2020-12-10T16:07:55.121395900'
  nodeSuccessor: Node_0
  nodePredecessor: Node_2
  nodeFingerTable:
  - start: '168'
    interval:
    - 168
    - 169
    succ: Node_0
  - start: '169'
    interval:
    - 169
    - 171
    succ: Node_0
  - start: '171'
    interval:
    - 171
    - 175
    succ: Node_0
  - start: '175'
    interval:
    - 175
    - 183
    succ: Node_0
  - start: '183'
    interval:
    - 183
    - 199
    succ: Node_0
  - start: '199'
    interval:
    - 199
    - 231
    succ: Node_0
  - start: '231'
    interval:
    - 231
    - 39
    succ: Node_0
  - start: '39'
    interval:
    - 39
    - 167
    succ: Node_0
  nodeID: 167
```


#### Future Improvements
  
* The Program does not scale well beyond 256 nodes. Therefore we have limited the max nodes to be 256 ie 2^8 or m = 8
* The Application was not performance tested. Hence, we have added a lot of timeouts between each line of code to add a small delay between each HTTP requests from the user. The application behaves poorly with rapid bursts (spike testing) or with heavy load for a large amount of time(Stress testing)
* The HTTP server method used is deprcated, the newer method did not bind the address of the sharded HTTP Server, this will be updated in the future
* As the documentation for typed Akka was sparse, there were some adjustments made to while sharding. All the courses taken by members were on classic model, therefore, some tradeoffs had to be made. 
  
