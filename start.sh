#!/bin/bash

function run
{
	local REQUIRED_ARG_COUNT=3

	if [ $# -lt $REQUIRED_ARG_COUNT ]
	then
		echo "this script requires three parameters. not enough parameters provided"
		echo "[name] [server] [port]"
	else
		echo "Starting bot with name $1"
		echo "Connecting to $2:$3"
		java -cp out/production/pongbot/ redlynx/bots/magmus/Magmus $1 $2 $3 &
		echo $! > .pids
	fi
}

run "$@"
