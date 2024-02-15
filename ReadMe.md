# Messaging Overlay Network Implementation

This repository contains my implementation of a messaging overlay network, completed as part of my homework assignment. The project is built using Gradle, and it includes test files used to validate the functionality of various classes.

## Features Implemented
- Messaging nodes communication
- Registration and deregistration with a registry
- Calculation of shortest paths using Dijkstra's algorithm
- Sending and receiving messages between nodes
- Traffic summary reporting

## Known Issues
While most features are implemented and functional, there are some unexpected behaviors observed, particularly with routing and sending packages. As a result, the traffic summary may not always return accurate results.

## Debug Mode
The application includes a debug mode for both the registry and messaging nodes. To enable debug mode, simply specify `--DebugMode` in the command line when running the application. This will print out additional debugging information about the messages being sent and received, aiding in troubleshooting.
Attached is my testing script ./testScript.sh which uses tmux to start 10 messaging nodes
## Usage
To run the messaging overlay network, execute the following command:
```bash
java MessagingNode <registry host> <registry port> [--DebugMode]
java Registry <port> [--DebugMode]
Keep in mind I used VScode to run the application, so the command may vary depending on the IDE used.
```
