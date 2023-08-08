# Configuration files for docker compose test environment.
To generate a test environment in Docker we have crafted configuration for 
two `nsi-dds` instances, an `opennsa` configuration for document discovery, 
and a database instance for use by `opennsa`.  We exercise NSA and Topology 
discovery using the Gof3 mechanism from `nsi-dds-east` and the peer discovery
protocol between `nsi-dds-east` and `nsi-dds-west`.


     nsi-dds-east <----> nsi-dds-west
          |
          |
       opennsa --> opennsa-db

* `nsi-dds-east-config` is configuration for the `nsi-dds-east` instance.
* `nsi-dds-west-config` is configuration for the `nsi-dds-west` instance.
* `opennsa-config` is configuration for the `opennsa` instance.
* `01-databases.sql` is configuration for the `postgres` database.
* `02-schema.sql` is the schema to load in the `postgres` database.

