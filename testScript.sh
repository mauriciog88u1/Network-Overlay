#!/bin/bash

cd /s/chopin/n/under/maur88/cs455/hw1/HW1_ATTEMPT_2

./gradlew build

tmux new-session -d -s "ssh_sessions"

command="/usr/bin/env /usr/lib/jvm/java-11-openjdk-11.0.21.0.9-2.el8.x86_64/bin/java -cp /s/chopin/n/under/maur88/cs455/hw1/HW1_ATTEMPT_2/build/resources/main:/s/chopin/n/under/maur88/cs455/hw1/HW1_ATTEMPT_2/build/classes/java/main csx55.overlay.node.MessagingNode sardine 1024 "

machines=("bogota" "breckenridge" "buttermilk" "cooper" "copper-mtn" "crested-butte" "eldora" "keystone" "monarch" "steamboat")

for machine in "${machines[@]}"; do
    tmux new-window -t "ssh_sessions" -n "$machine" "ssh $machine \"$command\"; read -p 'Press enter to close this session...'"
done

tmux attach-session -t "ssh_sessions"
