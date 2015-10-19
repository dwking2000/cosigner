FROM java:8
MAINTAINER Tom Robichaud <tom@emax.io>

ADD target/ /opt/emax/

WORKDIR /opt/emax

EXPOSE 8080

ENTRYPOINT ["java", "-server", "-jar", "cosigner-core-0.0.1-SNAPSHOT.jar", "server", "core.yml"]
