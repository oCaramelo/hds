# DepChain

## Introduction

Dependable Chain (DepChain) is a simplified permissioned blockchain system designed for high reliability and Byzantine fault tolerance in a closed membership environment where all participants have predefined cryptographic identities.

DepChain ensures:
- Reliable transaction processing, including cryptocurrency transfers and smart contract execution.
- Resilience against Byzantine faults, tolerating malicious or arbitrary node behavior.

The consensus protocol is based on a Byzantine Read/Write Epoch Consensus algorithm, tailored for static membership and assuming a correct leader. It guarantees safety under all conditions and liveness when the leader is correct.

The system comprises four nodes maintaining consensus and blockchain state and two clients generating and submitting transactions. Nodes communicate via authenticated perfect links and run full blockchain stacks, while clients handle key generation and transaction creation.

DepChain offers a modular and extensible foundation for dependable distributed applications.

---

## Table of Contents

- [Introduction](#introduction)
- [Usage Guide](#usage-guide)
- [To Be Improved](#to-be-improved)

---

## Usage Guide

### Requirements

The following software is required to run the project:

- [Java 21](https://openjdk.org/projects/jdk/21/) - Programming language
- [Maven 3.8.1](https://maven.apache.org/) - Build and dependency management tool
- [Python 3](https://www.python.org/downloads/) - Programming language
- [Kitty](https://sw.kovidgoyal.net/kitty/) - Terminal emulator for launching nodes and clients

### Configuration

To run the project, you need to configure files for:

* nodes configuration - located in `service/src/main/resources/`
* client configuration - located in `client/src/main/resources/`

Each node has a configuration object that contains the following fields:

```json
{
  "id": "<NODE_ID>",
  "isLeader": "<IS_LEADER>",
  "hostname": "<NODE_HOSTNAME>",
  "port": "<NODE_PORT>",
  "clientPort": "<CLIENT_PORT>",
  "privateKeyPath": "<PRIVATE_KEY_PATH>",
  "publicKeyPath": "<PUBLIC_KEY_PATH>",
  "behavior": "<NODE_BEHAVIOR>"
}
```

The client configuration object contains the following fields:

```json
{
  "id": "<CLIENT_ID>",
  "hostname": "<CLIENT_HOSTNAME>",
  "port": "<CLIENT_SERVER_PORT>",
  "privateKeyPath": "<PRIVATE_KEY_PATH>",
  "publicKeyPath": "<PUBLIC_KEY_PATH>",
  "address": "<CLIENT_ADDRESS>",
  "behavior": "<CLIENT_BEHAVIOR>"
}
```

### Generating Keys

To generate the keys for the nodes and clients, you can use the script `KeyPairGen.py`.
There you can define the number of nodes and clients, and the script will generate the necessary keys inside the `keypairs` folder.

You can run the script with:

```bash
cd shared/
mvn clean package
mvn exec:java -Dexec.mainClass="tecnico.keys.KeyPairGen"
```

### Dependencies

To install the necessary dependencies run the following command:

```bash
./install_deps.sh
```

### Running DepChain
The system is orchestrated using the puppet-master.py script, which launches nodes and clients in separate terminal windows via Kitty. Configuration files (client_default_config.json, server_default_config.json, etc.) define network IDs, ports, and Byzantine behaviors.

```bash
python puppet-master.py
```
This script validates configuration files, compiles the project, and starts four nodes and two clients. Nodes run the consensus protocol, while clients generate and submit transactions.
Byzantine Fault Injection: To test fault tolerance, modify node configurations in service/src/main/resources/ (e.g., server_drop_config.json) to activate behaviors like DROP, INVALID SIGNATURE, WRONG BLOCK or DELAY.


## To Be Improved
- The current system lacks leader election: a static leader is assumed.

 - Timers are partially implemented and not currently sending messages as expected, but this does not affect core functionality.

- Byzantine behavior simulation is configurable via JSON files.

- The class Instance exists to manage the blockchain state but dynamic user creation and instance discovery are not implemented.

