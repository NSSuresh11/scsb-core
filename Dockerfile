FROM scsb-base as builder
WORKDIR application
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} scsb-core.jar
RUN java -Djarmode=layertools -jar scsb-core.jar extract

FROM scsb-base

WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/scsb-core.jar/ ./
COPY /data/config/ssl-com-root-2022.pem /tmp/ssl-com-root-2022.pem

ENTRYPOINT java -jar -Denvironment=$ENV scsb-core.jar && bash

VOLUME /data/config/ssl-com-root-2022.pem:scsb-core:/var/ssl-com-root-2022.pem:rw
RUN keytool -importcert -noprompt -trustcacerts -cacerts \
      -alias ssl-com-tls-rsa-root-2022 \
      -file /var/ssl-com-root-2022.pem \
      -storepass changeit \
&& rm /var/ssl-com-root-2022.pem