# NSI Path Computation Engine

The Network Services Interface Path Computation Engine (NSI-PCE) is a conglomeration of components from the NSI 2.0 series of specifications [OGF NSF]:

* PCE Core - provides a RESTful interface for requesting path resolution within a network of NSI Connection Service enabled domains.  It utilizes the Spring Framework to provide an infrastructure for pluggable path computation algorithms, allowing users to incorporate their own path computation algorithms into the PCE.

* NSI Topology Service – provides an NSI Topology Representation [OGF NSI-TS] compliant service with an independent RESTful interface giving access to discovered network topology.

* NSI DDS – A full implementation of the proposed NSI Document Discover Service [OGF NSI-DS] with backwards-compatible support for dynamic discovery of  both A-GOLE and Gang-of-Three (Gof3) topology.

![Image of PCE](https://raw.githubusercontent.com/BandwidthOnDemand/nsi-pce/master/images/pce.png)

In the near future the NSI-PCE will be decoupled into two separate runtime packages, the NSI-PCE and the NSI-DDS, however, for the near future they are contained in a single runtime.

## Getting Started

The NSI-PCE utilizes maven as a build environment.  Once you have downloaded the `nsi-pce` project just type `maven clean install` in the project directory to build the NSI-PCE application.  The project includes a set of REST API test cases that demonstrate the use of available REST API.  To bypass running of the test suite during build include the maven skip test option `mvn install -Dmaven.test.skip=true`.
```
> git clone https://github.com/BandwidthOnDemand/nsi-pce.git
Cloning into 'nsi-pce'...
remote: Reusing existing pack: 4508, done.
remote: Counting objects: 11, done.
remote: Compressing objects: 100% (10/10), done.
remote: Total 4519 (delta 4), reused 0 (delta 0)
Receiving objects: 100% (4519/4519), 1.16 MiB | 541.00 KiB/s, done.
Resolving deltas: 100% (2171/2171), done.
Checking connectivity... done.
> cd nsi-pce
> mvm clean install
```

## Quickstart Configuration

If the NSI-PCE is being used in conjunction with a collocated NSI-Safnari NSA aggregator instance, then all configuration files can remain at their defaults, except for the DDS configuration `nsi-pce/config/dds.xml` which will need to be configured with the peer NSA information.  See the section titled *"DDS configuration (dds.xml)"* for more information.

Once configured, run the command `./run.sh` from within the `nsi-pce` directory.  The NSI-PCE will start up and discover the network.

## Configuring

The default runtime configuration directory for the NSI-PCE is `nsi-pce/config` but this can be changed through command line input.  There are three files that will need to be changed for a customized installation.

* http.json
* topology-dds.xml
* dds.xml

Alternatives to these files can also be specified on the command line.  The contents of each file will be discussed in their individual sections.

### HTTP configuration (http.json)

Configuration of the imbedded HTTP server is controlled through the `nsi-pce/config/http.json` file.  This file contains a simple JSON formatted list of configuration parameters:

```
{
    "pce": {
        "url": "http://localhost:8400/",
        "packageName": "net.es.nsi.pce",
        "staticPath": "config/www/",
        "wwwPath": "/www"
    }
}
```

* *url* - This is the public facing URL that applications will use to access the NSI-PCE RESTful interface.  If the NSI-PCE will only be accessed by application on localhost, then this URL can remain `http://localhost:8400/`, otherwise it will need to be changed to the public hostname.

* *packageName* - This is the package name of NSI-PCE module and the root from which the HTTP service will scan for available REST interfaces.  This should not be changed.

* *staticPath* - The NSI-PCE can be used to serve static content by specifying a local directory that will contain the content to be served.  This can be helpful when testing the DDS A-GOLE or Gof3 doscovery mechanism.  In most cases you will leave this as an empty string, thereby disabling the static content feature.

* *wwwPath* - The relative root path URL exposed through the NSI-PCE for static content.  In this example, the absolute URL for static content will be `http://localhost:8400/www`.  A path must be specified is the *staticPath* parameter is specified.  The relative path cannot be an empty string or the string "/".

### Topology source configuration (topology-dds.xml)

The NSI-PCE's internal topology service utilizes the NSI-DDS service (NSI Document Discovery Service v1) for collection of NSA Description and NML Topology documents from all NSA with the network.  The internal topology service is currently using the RESTful polling interface of the NSI-DDS, instead of the github manifest as supported in Topology Discovery v1.

The prototype NSI-DDS implementation used by the topology service is currently embedded within the NSI-PCE, but this will be moved to a separate process in the near future.  The topology source configuration is controlled through the `nsi-pce/config/topology-dds.xml` file.  This file contains a simple XML formatted list of configuration parameters:

```
<?xml version="1.0" encoding="UTF-8"?>
<top:topology xmlns:top="http://schemas.es.net/nsi/2013/08/pce/topology/configuration">
    <location>http://localhost:8400/discovery</location>
	<auditInterval>300</auditInterval>
    <defaultServiceType>http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE</defaultServiceType>
</top:topology>
```

* *location* - The base URL of the NSI DDS service used as a document source for NSA description and NML topology documents.  This will point to the localhost endpoint associated with the imbedded instance of the DDS service (same port as NSI-PCE interface).

* *auditInterval* - The interval (in seconds) the PCE topology service will audit the configured DDS server for document changes.  At the moment is controls the polling interval.

* *defaultServiceType* - The service type used as a default for networks not announcing a service type in NML topology.  If not present, this value will be used to create a default *ServiceDefinition* entry and *ServiceDomain* entries in the NSI topology for the discovered network.

### DDS configuration (dds.xml)

The NSI-PCE's internal NSI-DDS engine (NSI Document Discovery Service v1) is used for collection of NSA Description and NML Topology documents from all NSA with the network.  The DDS then makes these discovered documents available for application use thfought its RESTful API.  The NSI-PCE's internal topology services utilizes this DDS RESTful API for building a network topology view.

The `nsi-pce/config/dds.xml` configuration file controls runtime configuration of the NSI-DDS discovery engine.  The following XML elements are supported in this configuration file:
    
```
<?xml version="1.0" encoding="UTF-8"?>
<tns:discovery xmlns:tns="http://schemas.es.net/nsi/2014/03/pce/discovery/configuration"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <nsaId>urn:ogf:network:surfnet.nl:1990:nsa:bod-acc</nsaId>
    <documents>config/documents</documents>
    <cache>config/cache</cache>
    <auditInterval>300</auditInterval>
    <expiryInterval>600</expiryInterval>
    <actorPool>10</actorPool>
    <baseURL>http://localhost:8400/discovery</baseURL>
    <peerURL type="application/vnd.ogf.nsi.dds.v1+xml">
        http://dds.example.com:8400/discovery
    </peerURL>
    <peerURL type="application/vnd.ogf.nsi.nsa.v1+xml">
        https://nsa1.example.com/nsa-discovery
    </peerURL>
    <peerURL type="application/vnd.ogf.nsi.topology.v1+xml">
        https://example.com/master/manifest.xml
    </peerURL>
</tns:discovery>
```
    
* **nsaId** - The NSA identifier of the local NSA assocated with this DDS instance.  This value will be used to determine which documents in the DDS document space are associated with the `/local` URL query.  The NSI-PCE topology service utilizes the `/local` query to populate local NSA information. 
            
* **documents** - The local directory the DDS will monitor for document file content to auto load into the DDS document space.  This directory is checked for new content every auditInterval.  This element is optional.
                
* **cache** - The local directory used to store discovered documents that will be reloaded after a restart of the DDS.  One reloaded an audit occurs to refresh any documents with new versions available.  This element is optional.
            
* **auditInterval** - The interval (in seconds) the DDS will audit all peer DDS servers, Gof3 NSA and topology documents, or A-GOLE topology.
                    
* **expiryInterval** - The number of seconds the DDS will maintain a document after the document's lifetime has been reached.

* **actorPool** - The number of actors to instantiate per discovery type (DDS, Gof3, A-GOLE).
                
* **baseURL** - The base URL of the local DDS service that will be used when registering with peer DDS services.  Is only needed if a peerURL type of "application/vnd.ogf.nsi.dds.v1+xml" is configured.
              
* **peerURL** - Lists URL for peer data sources for the DDS service to utilize for document discovery. The following type of peerURL are supported (mixed types are supported):

```
application/vnd.ogf.nsi.dds.v1+xml
```

- A peer DDS server supporting NSI-DDS v1.  Each peer DDS server must have its own peerURL entry.
    
```
application/vnd.ogf.nsi.nsa.v1+xml
```

- An NSA supporting the Gof3 discovery protocol.  Each peer NSA must have its own peerURL entry.

```
application/vnd.ogf.nsi.topology.v1+xml
```
- The Automated GOLE topology discovery v1 which utilizes a manifest file.

## Running the NSI-PCE

```
#!/bin/bash

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR" >/dev/null; pwd`

java -Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true  \
	-Dapp.home="$BASEDIR" \
	-Dbasedir="$BASEDIR" \
	-Djava.util.logging.config.file="$BASEDIR/config/logging.properties" \
	-Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
	-Djavax.net.ssl.trustStore=config/nsi-pce-truststore \
	-Djavax.net.ssl.trustStorePassword=changeit \
	-jar target/pce.jar \
	-topologyConfigFile config/topology-dds.xml \
	-ddsConfigFile config/dds.xml \
	$*
```
# References

**[OGF NSF]** Guy Roberts, et al. “OGF Network Service Framework v2.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), January 28, 2014.

**[OGF NSI-CS]** Guy Roberts, et al. “OGF NSI Connection Service v2.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), January 12, 2014.

**[OGF NSI-TS]** Jeroen van der Ham, GWD-R-P Network Service Interface Topology Representation, Group Working Draft (GWD), candidate Recommendations Proposed (R-P), January 2013.

**[OGF NSI-DS]** John MacAuley, et al. “Network Service Interface Discovery Protocol v1.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), February 18, 2014.

**[OGF NSI-DS]** John MacAuley, et al. “Network Service Interface NSA Description Document v1.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), February 16, 2014.

**[OGF NML]** OGF GFD.206: Network Markup Language Base Schema version 1, http://www.gridforum.org/documents/GFD.206.pdf
