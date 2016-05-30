FROM cosigner_cosigner
MAINTAINER Tom Robichaud <tom@emax.io>

RUN mkdir -p /opt/functional/

RUN apt-get update
RUN apt-get install -y openssl git maven curl
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

RUN git clone https://github.com/EMAXio/cosigner.git /opt/functional/cosigner/
RUN git clone https://github.com/EMAXio/cosigner-client-java.git /opt/functional/cosigner-client
RUN cd /opt/functional/cosigner && mvn install -D skipTests
RUN cd /opt/functional/cosigner-client && mvn install -D skipTests

RUN cp /opt/emax/cosigner.jks /opt/functional/cosigner-client/target/
RUN mv /opt/functional/cosigner-client/target/cosigner-client.properties /opt/functional/cosigner-client/target/cosigner-client.properties.original
RUN sed 's/localhost/${COSIGNER_PORT_8443_TCP_ADDR}/g' < /opt/functional/cosigner-client/target/cosigner-client.properties.original > /opt/functional/cosigner-client/target/cosigner-client.properties

RUN cp /opt/emax/*.properties /opt/emax/lib/
RUN cp /opt/emax/*.properties /opt/functional/cosigner-client/target/
RUN mkdir /opt/emax/lib/lib
RUN cp /opt/emax/lib/*.jar /opt/emax/lib/lib

RUN cd /usr/bin && wget -O jq https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64
RUN chmod 555 /usr/bin/jq

ADD cosigner-client.sh /opt/functional/cosigner-client.sh
RUN chmod 555 /opt/functional/cosigner-client.sh

WORKDIR /opt/functional

EXPOSE 5555
EXPOSE 5555/udp
EXPOSE 5556
EXPOSE 5556/udp
EXPOSE 8080
EXPOSE 8443
EXPOSE 8445

ENTRYPOINT ["./cosigner-client.sh"]
#ENTRYPOINT ["/bin/bash"]
