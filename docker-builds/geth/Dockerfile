FROM ubuntu:trusty
MAINTAINER Tom Robichaud <tom@emax.io>

RUN apt-get update
RUN apt-get -y install software-properties-common
RUN apt-get update
RUN apt-get -y install expect
RUN apt-get -y install git build-essential libgmp3-dev wget

RUN wget https://storage.googleapis.com/golang/go1.5.linux-amd64.tar.gz && sudo tar -C /usr/local -xzf go1.5.linux-amd64.tar.gz
RUN mkdir -p /opt/emax/build
RUN cd /opt/emax/build && git clone https://github.com/ethereum/go-ethereum.git
RUN cd /opt/emax/build/go-ethereum && git checkout develop && GOROOT=/usr/local/go PATH=$PATH:$GOROOT/bin make geth
RUN mkdir -p /opt/emax/bin
RUN cp /opt/emax/build/go-ethereum/build/bin/geth /opt/emax/bin/

ADD privatenet.sh /opt/emax/bin/gethprivatenet.sh
ADD attach.sh /opt/emax/bin/gethattach.sh
ADD clearlicenseprompt.sh /opt/emax/bin/clearlicenseprompt.sh
ADD createaccount.sh /opt/emax/bin/createaccount.sh

WORKDIR /opt/emax/bin

EXPOSE 30303
EXPOSE 8101

RUN "/opt/emax/bin/clearlicenseprompt.sh"
RUN "/opt/emax/bin/createaccount.sh"

ENTRYPOINT "/opt/emax/bin/gethprivatenet.sh"
