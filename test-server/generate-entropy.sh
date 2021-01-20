#!/bin/bash

# In a terminal 1, run curl "http://localhost:8090/tests/nominal/pub/run?n=1000000000&pause=500"
# In a terminal 2, run the generate-entropy.sh script

while true; do
  curl http://localhost:8090/tests/nominal/sub/reject
  sleep 20
  curl http://localhost:8090/tests/nominal/sub/accept
  sleep 30
done