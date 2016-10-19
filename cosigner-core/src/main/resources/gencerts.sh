#!/bin/bash

# Working dir
mkdir ./cawork
cd ./cawork

# Generate CA key and cert
openssl genrsa -aes256 -out ca.key -passout pass:cosigner 4096
#chmod 400 ca.key
openssl req -new -subj "/CN=cosigner" -x509 -days 3650 -key ca.key -sha256 -out ca.pem -passin pass:cosigner

# Generate Server key and cert, signed by CA.
openssl genrsa -out server.key -passout pass:cosigner 4096
# Edit the CN to use the server name, and the Alt IPs to the server's IPs
openssl req -subj "/CN=cosigner" -sha256 -new -key server.key -out server.csr -passin pass:cosigner
echo subjectAltName = IP:10.0.3.13 > extfile.cnf
openssl x509 -req -days 3650 -sha256 -in server.csr -CA ca.pem -CAkey ca.key  -CAcreateserial -out server.pem -extfile extfile.cnf \
             -passin pass:cosigner

# Import the CA cert
keytool -import -keystore cosigner.jks -alias cosigner-ca -file ca.pem -trustcacerts -noprompt -storepass cosigner -keypass cosigner

# Merge the key and cert into a PKCS#12 and import it into the keystore
openssl pkcs12 -export -in server.pem -inkey server.key -out cosigner.p12 -name cosigner -CAfile ca.pem -caname root -passout pass:cosigner \
               -passin pass:cosigner
keytool -importkeystore -trustcacerts -deststorepass cosigner -destkeypass cosigner -destkeystore cosigner.jks \
        -srckeystore cosigner.p12 -srcstoretype PKCS12 -srcstorepass cosigner -alias cosigner -storepass cosigner -keypass cosigner
mv ./cosigner.jks ../

