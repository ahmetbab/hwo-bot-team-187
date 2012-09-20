#!/bin/bash
while read line
do
    kill -9 $line > /dev/null 2>&1;
done < .pids
rm .pids
