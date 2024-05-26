# User Storage User Interface for CANFAR

A fully functional UI is deployed at [CANFAR.net](https://www.canfar.net/storage/list/) (https://www.canfar.net/storage/list/).

## Building

Executing:

`./gradlew --info clean build`

Will produce a `war` file in the `build/libs` directory that can be deployed into a Java container such as Tomcat or Jetty.

## Configuration
User Storage requires a properties file be available, named `org.opencadc.vosui.properties`. 
Both the VOSpace web service implementation and the Files web service User Storage uses must be configured before
running this UI.

### VOSpace implementation 
To configure the VOSpace implementation User Storage should use, the org.opencadc.vosui.properties file should 
contain the following entries:

```properties
# Properties required for interacting with a VOSpace web service
# Default vospace service to display and query
org.opencadc.vosui.service.default = <service_name>

# For each backend service available (used in a pulldown on the page)
org.opencadc.vosui.service.name = <service_name>

# The resource id of the VOSpace web service to use
org.opencadc.vosui.<service_name>.service.resourceid = <URI that identifies the VOSPace web service>

# Base URI to use as node identifier
org.opencadc.vosui.<service_name>.node.resourceid = <URI that is the base of node identifiers>

# Base home directory for authenticated users
org.opencadc.vosui.<service_name>.user.home = <relative path, starting with '/'>

# Features for this service.
# batchDownload: true/false - Whether the batch downloadManager service is available for batch downloads.
# batchUpload: true/false - Whether the batch downloadManager service is available for batch downloads.
# externalLinks: false - Whether this service supports creating hyperlinks (external to the system), such as http(s) links or ftp links.  File systems do not support this.
# paging: false - Whether this VOSpace service supports the limit=<int> and startURI=<uri> features.
org.opencadc.vosui.<service_name>.service.features.batchDownload = <true / false>
org.opencadc.vosui.<service_name>.service.features.batchUpload = <true / false>
org.opencadc.vosui.<service_name>.service.features.externalLinks = <true / false>
org.opencadc.vosui.<service_name>.service.features.paging = <true / false>

# Note: replace <service_name> with the name of the VOSpace implementation in all cases, ie `vault` or `cavern`.
# END: For each backend service available (used in a pulldown on the page)

org.opencadc.vosui.theme.name = canfar

# For OpenID Connect support.
org.opencadc.vosui.oidc.clientID = <openid connect client id>
org.opencadc.vosui.oidc.clientSecret = <openid connect client secret>
org.opencadc.vosui.oidc.redirectURI = <uri to callback to AFTER successful login from the OpenID Connect Provider>
org.opencadc.vosui.oidc.callbackURI = <uri to callback to AFTER successful login and AFTER the redirectURI>
org.opencadc.vosui.oidc.scope = <space separated scopes to send to the OpenID Connect provider>

# Token cache for storing tokens with OpenID Connect.  Specify URL here.
org.opencadc.vosui.tokenCache.url = redis://localhost:6379
# END: For OpenID Connect support.
```

### Running

#### Environment

In order for Authorization and Authentication to work properly, the User Storage UI uses Domain Cookies to pass from the browser to the application.
The browser will always send a valid cookie to the server whose domain matches the cookie's, but the application will then make another request
to the VOSpace Web Service using the cookie.  The VOSpace Web Service can run on a different domain, however, which presents a problem for
cookie passing.

To get around this, please supply a property called `SSO_SERVERS` containing a space delimited list of trusted servers one of two ways:

  - As a System property (e.g. `-DSSO_SERVERS="<host 1> <host 2>" etc.`)
  - In a file located at `${user.home}/config/AccessControl.properties` (e.g. `cat SSO_SERVERS=<host 1> <host 2> > $HOME/config/AccessControl.properties`)
  - Supply the System property `-Dorg.opencadc.vosui.mode=dev` to run with a default UI.  The alternative will require CANFAR decorations.  This is useful for development testing.

The hostnames included in this property are all the servers involved in your setup (i.e. the web server, and the VOSpace Web Service host).

#### Deployment

To produce a running embedded Tomcat container running on port `8080`, with a debug port on `5555`, deploy with Docker:

Pass your own Registry settings into the `JAVA_OPTS` environment variable to use your own VOSpace service:

```
docker run --rm -d -p 8080:8080 -e CATALINA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5555 -Djava.security.egd=file:/dev/./urandom -Djsse.enableSNIExtension=false -Dca.nrc.cadc.reg.client.RegistryClient.host=<your host for IVOA Registry host>" tomcat:9-jdk11-openjdk-slim
```

To specify the Service ID (often called Resource ID) of your services.  The User Storage Interface relies on two services:

 - A VOSpace web service implementation (e.g. `vault` or `cavern`)
 - Group Management Service (Access Control)
 
 See the 'Configuration' section for how to set up access to a VOSpace implementation. 

To specify the Service ID for Group Management, add the appropriate entry in the `cadc-registry.properties`:

ivo://ivoa.net/std/GMS#search-1.0 = ivo://<your authority domain>/<gms service name>

Or deploy the `war` file in `build/libs` into a Java container such as Tomcat.

Then, in your browser, look at <a href="http://localhost:8080/storage/list">http://localhost:8080/storage/list</a>.

#### Browser access

The endpoints are in the format of `/{contextPath}/{serviceName}/{action}/{path}`, where:

| Path item     | Description                                                                                                                                                                                                             |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `contextPath` | The name of the deployed application.  The default is `storage`                                                                                                                                                         |
| `serviceName` | The name of the currently selected VOSpace backend service, as it appears in the pulldown menu and in the configuration file (e.g. `cavern`).  If this is omitted, the default value in the configuration file is used. |
| `action`      | The item being requested.  Values are `folder`, `file`, `link`, `list`, and `page`                                                                                                                                      |
| `path`        | The path of the currently visited folder, or the item being requested from a non-`list` request                                                                                                                         |

For example, the default context path is `/storage`, so to get the list of items in the `/my/folder/path` folder:
```shell
open https://example.org/storage/myvospace/list/my/folder/path
```

To check the quota of a folder:
```shell
open https://example.org/storage/myvospace/folder/my/folder/path
```

#### Running with Docker

See the [OpenCADC Docker repository](https://images.opencadc.org/harbor/projects/7/repositories/storage-ui)

It extends the [OpenCADC Tomcat container](https://github.com/opencadc/docker-base/tree/master/cadc-tomcat)

##### Running with Docker for your environment

To run in your environment, create your own Dockerfile:

```
# This is the Docker hub location for the User Storage User Interface
FROM tomcat:9-jdk11-openjdk-slim

# The JAVA_OPTS variable to pass to Tomcat.  Note the -Dca.nrc.cadc.reg.client.RegistryClient.host property.
ENV JAVA_OPTS "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5555 -Djava.security.egd=file:/dev/./urandom -Djsse.enableSNIExtension=false -Dca.nrc.cadc.reg.client.RegistryClient.host=<your host for IVOA Registry lookup>"
```

Then package it:

`docker build -t user_storage_ui .`

Then run it:

`docker run --name storage -d -p 8080:8080 -p 5555:5555 user_storage_ui`
