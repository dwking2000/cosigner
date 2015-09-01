FROM docker.internal.emax.io/java8
MAINTAINER Tom Robichaud <tom@emax.io>

RUN apt-get update
RUN apt-get -y install software-properties-common
RUN add-apt-repository -y ppa:ethereum/ethereum
RUN apt-get update
RUN apt-get -y install ethereum
RUN mkdir -p /opt/emax/bin

ADD private_genesis.json /opt/emax/geth/private_genesis.json
ADD privatenet.sh /opt/emax/bin/gethprivatenet.sh
ADD createaccount.sh /opt/emax/bin/gethcreateaccount.sh

WORKDIR /opt/emax/bin

EXPOSE 30301
EXPOSE 8101

RUN "/opt/emax/bin/gethcreateaccount.sh"

ENTRYPOINT "/opt/emax/bin/gethprivatenet.sh"

