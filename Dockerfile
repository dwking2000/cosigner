FROM java:8
MAINTAINER Tom Robichaud <tom@emax.io>

RUN cd /usr/bin && wget -O jq https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64
RUN chmod 555 /usr/bin/jq

ADD target/ /opt/emax/
ADD ./docker-builds/cosigner-scripts/startapp.sh /opt/emax/startapp.sh
RUN chmod 555 /opt/emax/startapp.sh
WORKDIR /opt/emax

EXPOSE 5555
EXPOSE 5555/udp
EXPOSE 5556
EXPOSE 5556/udp
EXPOSE 8080
EXPOSE 8443

ENTRYPOINT ["./startapp.sh"]
