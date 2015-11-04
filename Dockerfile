FROM java:8
MAINTAINER Tom Robichaud <tom@emax.io>

ADD target/ /opt/emax/

WORKDIR /opt/emax

EXPOSE 5555
EXPOSE 5555/udp
EXPOSE 5556
EXPOSE 5556/udp
EXPOSE 8080
EXPOSE 8443

ENTRYPOINT ["java", "-server", "-jar", "cosigner-core-0.0.1-SNAPSHOT.jar", "server", "core.yml"]
