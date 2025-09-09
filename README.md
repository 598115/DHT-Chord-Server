# DHT Cooperative Mirroring with Consistency and Distributed Mutual Exclusion

This project is a fully functional implementation of a Chord-based Distributed Hash Table (DHT) with integrated support for cooperative mirroring, replica consistency, and a distributed mutual exclusion protocol. Built on top of Java RMI, it provides a robust peer-to-peer file distribution and management system, suitable for exploring core concepts in distributed systems such as replication, consistency, and coordination.

## 🧠 Core Features
### 🔁 Distributed Hash Table (DHT) - Chord Protocol

At its foundation, the system uses the Chord algorithm to form a structured peer-to-peer ring where each peer (node) is responsible for a range of keys in a 128-bit address space (MD5-based). Nodes maintain finger tables to enable efficient lookup of keys in O(log N) time.

### 📁 Cooperative File Mirroring

Files are split into multiple replicas that are evenly distributed across participating peers. Each file is dynamically replicated (default 4 replicas) and assigned to nodes based on hash values. This provides:

- High availability

- Fault tolerance

- Load distribution

### ✅ Sequential Consistency with Remote-Write Protocol

Consistency is enforced using a remote-write protocol, where one node is randomly assigned as the primary replica owner. All update operations are routed through the primary, which then ensures the changes propagate to all other replicas, maintaining sequential consistency across the system.

### 🔒 Distributed Mutual Exclusion

The system implements a distributed mutual exclusion algorithm that allows nodes to coordinate access to critical sections — particularly useful during concurrent write operations. Nodes vote to grant or deny access to ensure only one update occurs at a time per file.

### 📡 Remote Communication with Java RMI

All inter-node communication is handled via Java Remote Method Invocation (RMI), allowing each node to expose and invoke remote procedures such as file replication, lookup, coordination, and updates.

### 🧩 System Architecture

The system is organized into modular Java packages:

- Chord Operations: Core Chord protocol implementations — stabilization, finger table updates, and lookup logic.

- Middleware: Main peer logic including file management, remote method definitions, mutual exclusion, and consistency coordination.

- Client Nodes: A set of preconfigured Java classes (Process1 to Process5) that simulate individual peers on a single machine. These can easily be extended to a real distributed network using IP addresses.

- Utilities: Hashing (MD5), address space calculations, replica logic, and interval checks for ring navigation.

- GUI Interface: A lightweight graphical interface to visualize ring structure and simulate file operations (join, lookup, update, distribute).

### 🖼️ Visual Overview
- Chord Ring with Finger Tables

- Cooperative Mirroring Architecture

- Remote-Write Protocol

- Distributed Mutual Exclusion

### ⚙️ Technical Assumptions & Design Constraints

- Network Model: Reliable, synchronous communication. No message loss or delays.

- Consistency Model: Strict sequential consistency using replicated-write protocol.

- Topology: Fixed number of peer processes forming a Chord ring.

- Environment: Simulated on a single machine but adaptable to multi-node deployment via network configuration.

### 🔬 Usage Scenarios

- File Sharing: Efficient distribution and retrieval of files across multiple nodes.

- High Availability Services: Redundant data storage ensures fault tolerance in peer failures.

- Research and Teaching Tool: Demonstrates distributed coordination, consistency models, and mutual exclusion in action.

- Simulation Environment: Useful for testing distributed algorithms with real synchronization logic.

### 🛠️ Built With

- Java

- Java RMI for remote method calls

- Maven for project management and testing

### 🧪 Testing

- The system includes a comprehensive suite of automated tests that validate:

- Hashing and key distribution

- Replica creation and placement

- Lookup resolution via finger tables

- Consistency enforcement via primary replica control

- Distributed coordination and mutual exclusion

Tests are run using:

- mvn test

### 📦 Deployment

While the current implementation simulates nodes on a single machine, the system is designed to scale easily to multiple devices by configuring nodenames as IP addresses instead of localhost identifiers.

### 📚 References

This system was inspired by core concepts from distributed systems literature, particularly:

- Chord: A Scalable Peer-to-peer Lookup Protocol for Internet Applications

- Tanenbaum & Van Steen – Distributed Systems: Principles and Paradigms

- Java RMI documentation

### Originally started as a univerity project at HVL as practice on distributed file system architechture
