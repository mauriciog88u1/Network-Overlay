#!/bin/bash

# Navigate to the project directory
cd /s/chopin/n/under/maur88/cs455/hw1/HW1_ATTEMPT_2

# Rebuild the project using Gradle
./gradlew build

# Start a new TMUX session
tmux new-session -d -s "ssh_sessions"

# Define the command to be executed on each machine
command="/usr/bin/env /usr/lib/jvm/java-11-openjdk-11.0.21.0.9-2.el8.x86_64/bin/java -cp /s/chopin/n/under/maur88/cs455/hw1/HW1_ATTEMPT_2/build/resources/main:/s/chopin/n/under/maur88/cs455/hw1/HW1_ATTEMPT_2/build/classes/java/main csx55.overlay.node.MessagingNode sardine 1024"

# Define the list of machines
machines=("bogota" "breckenridge" "buttermilk" "cooper" "copper-mtn" "crested-butte" "eldora" "keystone" "monarch" "steamboat")

# Loop through each machine and execute the command via SSH in a separate TMUX window
for machine in "${machines[@]}"; do
    tmux new-window -t "ssh_sessions" -n "$machine" "ssh $machine \"$command\"; read -p 'Press enter to close this session...'"
done

# Attach to the TMUX session to monitor the processes
tmux attach-session -t "ssh_sessions"

