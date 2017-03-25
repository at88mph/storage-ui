FROM tomcat:8.5-alpine

# Default options for the Java runtime.  Other CANFAR ones can include:
# -Dca.nrc.cadc.reg.client.RegistryClient.host=<your host for CANFAR registry entries>
ENV JAVA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5555 -Djava.security.egd=file:/dev/./urandom -Djsse.enableSNIExtension=false -Dca.nrc.cadc.auth.BasicX509TrustManager.trust=true"

COPY LocalAuthority.properties /root/config/
COPY *.war webapps/

EXPOSE 5555

