# NSI DDS Deployment
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

```
 Address A                                                                        Address B
------------                                                                     -----------
| Peer DDS |                      https://<Address B>/dds                        |   DDS   |
|  Server  | ------------------------------------------------------------------> |  Server |
------------                             secured                                 -----------
     ^                                                                                |
     |                            https://<Address A>/dds                             |
     ----------------------------------------------------------------------------------
                                         secured
```
The second supported deployment model is direct SSL communications exposed from the DDS Server.  The DDS server's SSL/TLS will be configured using the Java keystore, and authentication of the client is established using the Java truststore.  Authorization of the client is done using an internal mechanism specified in the configuration file (\<accessControl\> XML element).  This mechanism is described below, but in summary will allow an access control role of read, write, peer, or admin to be assigned for each client accessing the DDS server.

## Using Apache HTTPD as a reverse proxy


## Software prerequisites
The `nsi-dds` has a runtime dependency on Java ([Oracle-JDK](https://jdk8.java.net/download.html) or OpenJDK), with versions 1.8 through 15 supported.  If the system does not contain the correct version of Java then a supported version needs to be installed. 

Maven is also required to build the `nsi-dds` module so install if not available on the system.

The current version of the `nsi-dds` needs to be fronted by a **reverse proxy** to provide front end SSL support and access control enforcement.  Although any **reverse proxy** could be used to front the `nsi-dds` application, Apache's [`httpd`](https://httpd.apache.org) has all the required features and will be the reference **reverse proxy** within documentation.  At the moment,  `httpd-2.2.15` and supporting modules has been verified against `nsi-dds`.  If the system does not contain a correct version of both `httpd` and `mod_ssl` then a supported version needs to be installed.
