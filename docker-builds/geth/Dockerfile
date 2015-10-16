FROM ubuntu:trusty
MAINTAINER Tom Robichaud <tom@emax.io>

RUN apt-get update
RUN apt-get -y install software-properties-common 
RUN add-apt-repository -y ppa:ethereum/ethereum
RUN apt-get update
RUN apt-get -y install expect ethereum

ADD privatenet.sh /opt/emax/bin/gethprivatenet.sh
ADD attach.sh /opt/emax/bin/gethattach.sh
ADD clearlicenseprompt.sh /opt/emax/bin/clearlicenseprompt.sh
ADD createaccount.sh /opt/emax/bin/createaccount.sh
ADD private_genesis.json /opt/emax/geth/private_genesis.json

WORKDIR /opt/emax/bin

EXPOSE 30303
EXPOSE 8101

RUN "/opt/emax/bin/clearlicenseprompt.sh"
RUN "/opt/emax/bin/createaccount.sh"

ENTRYPOINT "/opt/emax/bin/gethprivatenet.sh"
