# NSI Document Distribution Service
To do a clean install of the `nsi-dds` application we will follow these high level steps:

  * Install software prerequisites.
  * Create an application specific user.
  * Download the `nsi-dds` source from Github.
  * Build the `nsi-dds` application source.
  * Install the `nsi-dds` application runtime.
  * Configure the `nsi-dds` runtime.
  * Configure TLS for secure communications.
  * Install the `nsi-dds` Upstart script allowing `initd` to managed the process lifecycle.
  * Configure Apache httpd mod_proxy as an TLS frontend.
 
## Deployment options
Currently the `nsi-dds` supports standalone communications via unprotected HTTP or protected HTTPS when fronted by Apache mod_proxy.  In a future release secure standalone communications will be supported.

Here is an example deployment diagram:

```
 Address A                               Address B                               localhost (B)
------------                           -------------                             -----------
| Peer DDS | https://<Address B>/dds   |   httpd   | http://localhost:8401/dds   |   DDS   |
|  Server  | ------------------------> | mod_proxy |---------------------------> |  Server |
------------      secured              -------------       unsecured             -----------
     ^                                                                                |
     |                            https://<Address A>/dds                             |
     ----------------------------------------------------------------------------------
                                         secured
```
On the front-end we have Apache httpd with mod_proxy configured to terminate HTTPS on the client side, and proxy HTTP via **localhost** to a protected HTTP port on the DDS server.  The DDS server itself can be configured to communicate with HTTPS back to the peer DDS server.  The peer DDS server dictates the use of HTTP versus HTTPS, but the local DDS server will need to have Java key and trust stores configured properly for SSL communications.

## Software prerequisites
The `nsi-dds` has a runtime dependency on Java ([Oracle-JDK](https://jdk8.java.net/download.html) or OpenJDK), with a recommended version of 1.8 or later.  If the system does not contain the correct version of Java then a supported version needs to be installed.  

Maven is also required to build the `nsi-dds` module so install if not available on the system.

The current version of the `nsi-dds` needs to be fronted by a **reverse proxy** to provide front end SSL support and access control enforcement.  Although any **reverse proxy** could be used to front the `nsi-dds` application, Apache's [`httpd`](https://httpd.apache.org) has all the required features and will be the reference **reverse proxy** within documentation.  At the moment,  `httpd-2.2.15` and supporting modules has been verified against `nsi-dds`.  If the system does not contain a correct version of both `httpd` and `mod_ssl` then a supported version needs to be installed.

## Create an application specific user

The `nsi-dds` application should be run as a low-privilege user on the target production server. Ideally it should be run as a user created only for the purpose of running the set of software associated with the `nsi-dds` application.  For the remainder of this documentation it is assumed that we are downloading, building, and installing the `nsi-dds` software in the dedicated `safnari` user account.

Create a new user and user group for the `nsi-dds` application if you have not already done so (I.e. `safnari` for installation of `nsi-safnari` application):

```
$ groupadd safnari
$ useradd safnari -g safnari
$ sudo su - safnari
```

## Downloading the source
It is recommended a copy of the source is directly downloaded from the repository using the `git` command so that load tagging during the build process will add the appropriate revision information to your build.  This requires a properly cloned git repository.  Downloading using alternative methods will track the major and minor build versions, but not the specific version within the source code stream.

Perform the following commands in the directory you would like to download the source (for example, `/home/safnari/src/`):

    $ git clone https://github.com/BandwidthOnDemand/nsi-dds.git

If the download reports successful you now have the current version of the `nsi-dds` source code.  If you are downloading `nsi-safnari` and/or `nsi-pce` software packages make sure to get compatible versions.

## Building the source
These instructions assume you have downloaded the `nsi-dds` software package from GitHub and placed it in a common source directory `$SRCDIR` (I.e. `/home/safnari/src/`).  We recommend the built `nsi-dds` application is installed to its own writable runtime directory since caches files are stored under the `nsi-dds/config/cache` directory .  For these instructions (and for the example scripts provided) we will assume the `nsi-dds` applicaiton will be installed into the `/home/safnari/nsi-dds` directory.

You will need to be connected to the Internet when building for the first time as `mvn` will download any needed dependencies.

As application user (I.e. `safnari`):

  * From source directory `nsi-dds` build source: `mvn clean install`.
  * Copy configuration directory `nsi-dds/config` and `nsi-dds/target/dds.jar` to install location.
  * Configure `nsi-dds` runtime.
  * Copy the default DDS Upstart script from source directory `nsi-dds/scripts/nsi-dds.conf` to `/etc/init/nsi-dds.conf`.
  * Configure `/etc/init/nsi-dds.conf` for your DDS runtime configuration.

```
$ cd $SRCDIR/nsi-dds
$ mvn clean install -Dmaven.test.skip=true
$ cp -R config target/dds.jar /home/safnari/nsi-dds
```
Now you are ready to configure the `nsi-dds` runtime.

## Configuring the nsi-dds
Before begining the configuration of your `nsi-dds` instance make sure you have the following information available:

  * Public IP address/hostname for the `nsi-dds` server or fronting HTTP proxy.
  * The NSA identifier that will be assigned to this `nsi-dds` instance.  This will be the same as the colocated NSA, or a unique one if this is a standalone `nsi-dds` instance.
  * The URL of peer DDS instances.
  * The list of URL for any legacy uPA discovery mechanisms that you want to support.
  * Public and private keys for the `nsi-dds` server.
  * Public certificates for peer DDS and uPA you would like to trust.
  
We start with configuring the imbedded HTTP container.

### Configuring the imbedded HTTP container

If you need to change the address or port number for the HTTP container this can be done by editing the configuration file `config/http.json`:

```
{
    "dds": {
        "url": "http://localhost:8401/",
        "packageName": "net.es.nsi.dds",
        "staticPath": "config/www/",
        "wwwPath": "/www"
    }
}
```

When using the Apache `mod_proxy` module as a front end solution we can leave these values as is and point Apache to this internal port for DDS API access.  If this is a standalone instance change the URL to the specific IP address and port for `nsi-dds` access.  The address '0.0.0.0' will bind to all defined addresses including **localhost**.

### Configuring the DDS runtime and peer DDS servers
The basic runtime configuration of the `nsi-dds` is controlled through the `config/dds.xml` configuration file, or the `config/beans.xml` file.  We first start with `config/dds.xml` file:

`vi config/dds.xml`

    <tns:dds xmlns:tns="http://schemas.es.net/nsi/2014/03/dds/configuration"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <!-- we need to set this to the uPA's NSA ID so that the local networks are discovered correctly -->
        <nsaId>urn:ogf:network:icair.org:2013:nsa:nsi-am-sl</nsaId>
        <documents>config/documents</documents>
        <cache>config/cache</cache>
        <repository>config/repository</repository>
        <expiryInterval>600</expiryInterval>
        <baseURL>http://nsi-am-sl.northwestern.edu/dds</baseURL>

        <!-- DDS service interfaces. -->
        <peerURL type="application/vnd.ogf.nsi.dds.v1+xml">https://nsi-aggr-west.es.net/discovery</peerURL>
        <peerURL type="application/vnd.ogf.nsi.dds.v1+xml">https://agg.netherlight.net/dds</peerURL>

        <!-- Old AutoGOLE github discovery mechanism. -->
        <peerURL type="application/vnd.ogf.nsi.topology.v1+xml">https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/master.xml</peerURL>
    </tns:dds>

The following XML elements are supported in this configuration file:

  * nsaId - The NSA identifier of the local NSA assocated with this DDS instance.  This value will be used to determine which documents in the DDS document space are associated with the /local URL query.

  * documents - The local directory the DDS will monitor for document file content to auto load into the DDS document space.  This directory is checked for new content every auditInterval.

  * cache - The local directory used to store discovered documents that will be reloaded after a restart of the DDS.  One reloaded an audit occurs to refresh any documents with new versions available.
  
  * repository - The local directory where documents added to the local DDS are stored.  This is different from the cache directory in that the cache directory mirrors the state of the DDS document space, while the repository only holds those documents mastered ("owned") by this DDS server instance.

  * expiryInterval - The number of seconds the DDS will maintain a document after the document's lifetime has been reached.
  
  * baseURL - The base URL of the local DDS service that will be used when registering with peer DDS services.  Is only needed if a peerURL type of "application/vnd.ogf.nsi.dds.v1+xml" is configured.

  * peerURL - Lists peer data sources for the DDS service to utilize for document discovery.  The following type of peerURL are supported:

    ```
    application/vnd.ogf.nsi.dds.v1+xml - A peer DDS server.
    application/vnd.ogf.nsi.nsa.v1+xml - A Gof3 NSA.
    application/vnd.ogf.nsi.topology.v1+xml - The Automated GOLE topology discovery.
    ```

The `config/beans.xml` file contains a number of runtime configuration values for thread pool sizes and discovery audit timers.  These values should only be changed once you have a clear understanding of the different types of discovery mechanisms and their impact on the system.

### Configure logging
By default `nsi-dds` will log under the application home directory `/home/safnari/nsi-dds`.  If you would like to change this to log under the system `/var/log` directory then we need to modify the logging properties.

`vi log4j.xml`

    <param name="File" value="${basedir}/var/log/nsi-dds.err.log" />

This default will place in the runtime installation directory.  To put log files in `/var/log` modify each path component to something similar to:

     <param name="File" value="/var/log/nsi-dds/nsi-dds.err.log" />

Repeat for each of the log levels contained in the `log4j.xml` file.

Now for the default Java logger:

`vi logging.properties`

Change:

```
java.util.logging.FileHandler.pattern=var/log/jersey.log
```

To:

```
java.util.logging.FileHandler.pattern=/var/log/nsi-dds/jersey.log
```

Make sure to create the `/var/log/nsi-dds` logging directory if configuration was changed and `chown` logging directory to ownership of application account.

```
$ sudo mkdir /var/log/nsi-dds
$ sudo chown safnari.safnari /var/log/nsi-dds
```

## Configuring TLS
If you are using a standard `nsi-safnari` deployment then you will want to configure TLS communications.  In this version of `nsi-dds` it is assumed you will be used TLS with the key and trust stores specified on the command line using the standard Java parameters:

```
    -Djavax.net.ssl.trustStore=$TRUSTSTORE \
    -Djavax.net.ssl.trustStorePassword=$PASSWORD \
    -Djavax.net.ssl.keyStore=$KEYSTORE \
    -Djavax.net.ssl.keyStorePassword=$PASSWORD \
```
 
 Additional configuration options will be provided in the future.

## Command line parameters

```
java -jar dds.jar [-base <application directory>] [-config <configDir>] [-ddsConfigFile <filename>]
```
  * **-base** The runtime home directory for the application (defaults to "user.dir").
  
  * **-config** DDS configuration files (defaults to ${user.dir}/config).
  
  * **-ddsConfigFile** Path to your DDS configuration file.
  
As an alternative to these command line parameters, values can be specified using the following System properties:

  * **basedir** The runtime home directory for the application (defaults to "user.dir").
  
  * **configdir** DDS configuration files (defaults to ${user.dir}/config).
  
  * **ddsConfigFile** Path to your DDS configuration file.

## Install upstart scripts
If you would like `initd` to manage the process lifecycle of `nsi-dds` then you can install the provided upstart configuration file.

```
$ sudo cp $SRCDIR/nsi-dds/scripts/nsi-dds.conf /etc/init
```
Edit `/etc/init/nsi-dds.conf` to match your specific runtime configuration.


```
#!upstart

description "nsi-dds"

env USER=safnari
env GROUP=safnari
env HOME=/home/safnari/nsi-dds
env PORT="8401"
env ADDRESS="127.0.0.1"
env TRUSTSTORE=/home/safnari/jks/truststore.jks
env KEYSTORE=/home/safnari/jks/keystore.jks
env PASSWORD="changeit"

start on started postgresql-9.3
stop on stopping postgresql-9.3

respawn limit 10 5

script
  exec 2>>/var/log/nsi-dds/upstart.log
  set -x

[ -e /home/safnari/nsi-dds/dds.jar ]
exec su -l -s /bin/bash -c 'exec "$0" "$@"' $USER -- /usr/bin/java \
    -Xmx1024m -Djava.net.preferIPv4Stack=true  \
    -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
    -Djava.util.logging.config.file="$HOME/config/logging.properties" \
    -Djavax.net.ssl.trustStore=$TRUSTSTORE \
    -Djavax.net.ssl.trustStorePassword=$PASSWORD \
    -Djavax.net.ssl.keyStore=$KEYSTORE \
    -Djavax.net.ssl.keyStorePassword=$PASSWORD \
    -Dbasedir="$HOME" \
    -jar "$HOME/dds.jar" \
end script
```


