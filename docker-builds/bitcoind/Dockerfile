FROM ubuntu:trusty
MAINTAINER Tom Robichaud <tom@emax.io>

RUN apt-get update
RUN apt-get -y install software-properties-common
RUN apt-add-repository -y ppa:bitcoin/bitcoin
RUN apt-get update
RUN apt-get -y install bitcoind
RUN mkdir -p /opt/emax/bin

ADD privatenet.sh /opt/emax/bin/btcprivatenet.sh
ADD bitcoin.conf /root/.bitcoin/bitcoin.conf
RUN chmod 555 /opt/emax/bin/btcprivatenet.sh

WORKDIR /opt/emax/bin

EXPOSE 18332

ENTRYPOINT "/opt/emax/bin/btcprivatenet.sh"

