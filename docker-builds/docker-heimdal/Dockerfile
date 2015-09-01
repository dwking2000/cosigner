FROM docker.internal.emax.io/java8
MAINTAINER Matthew Morgan <hexfury@gmail.com>

ADD target /opt/emax/

WORKDIR /opt/emax

EXPOSE 8080

ENTRYPOINT ["/usr/bin/java", "-server", "-jar", "heimdal-core-0.0.1-SNAPSHOT.jar"]
