# How to run the functional tests on cosigner

1. Go to the base directory for the project (../../)
2. Build cosigner with ```mvn install```
3. Build the docker-compose images for cosigner with ```docker-compose build```
4. Come back to this directory and build the docker images with ```docker-compose build``` (They rely on the cosigner images)
5. Start the images with ```docker-compose up```
6. [**This will be fixed with a script later**] Unless you were lucky, cosigner did not beat the functional image to booting up. Kill the functional image with ```docker kill functionaltests_functional_1```, and restart it with ```docker-compose up```.
7. Read through the output from the functional_1 logs, which should be the last thing on the screen, verify that there were no exceptions and the results make sense. 
