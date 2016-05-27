#!/bin/bash

cd ../../
docker-compose pull
docker-compose build

cd ./docker-builds/functional-tests/
docker-compose pull
docker-compose build

docker-compose rm -f
docker-compose up 

