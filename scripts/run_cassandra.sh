#!/bin/bash

#Run this script to start up a local Cassandra instance for test/development.
docker run --rm -p 9042:9042 cassandra:latest
