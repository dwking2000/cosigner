FROM ubuntu:trusty
MAINTAINER Tom Robichaud <tom@emax.io>

RUN apt-get update
RUN apt-get -y install software-properties-common 
RUN add-apt-repository -y ppa:ethereum/ethereum
RUN apt-get update
RUN apt-get -y install expect ethereum

ADD privatenet.sh /opt/emax/bin/gethprivatenet.sh
RUN chmod 555 /opt/emax/bin/gethprivatenet.sh
ADD attach.sh /opt/emax/bin/gethattach.sh
RUN chmod 555 /opt/emax/bin/gethattach.sh
ADD createaccount.sh /opt/emax/bin/createaccount.sh
RUN chmod 555 /opt/emax/bin/createaccount.sh

WORKDIR /opt/emax/bin

EXPOSE 30303
EXPOSE 8101

RUN "/opt/emax/bin/createaccount.sh"

ENTRYPOINT "/opt/emax/bin/gethprivatenet.sh"
