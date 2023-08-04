# These files need to be under the sense-rm build context so they
# are available under the docker mount.
#KEYFILE="certificates/host.key"
#CERTFILE="certificates/host.cer"
#CAFILE="certificates/host.ca"

all:	build 

.PHONY: build test clean
build:
	docker run -it --rm --name dds-build \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		maven:3.8.1-openjdk-17-slim mvn clean install -DskipTests=true

test:
	docker run -it --rm --name dds-build \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		-p 8801:8801 \
		-p 8802:8802 \
		maven:3.8.1-openjdk-17-slim mvn clean install

# -Dtest=AgoleManifestReaderTest#loadMasterList

clean:
	docker run -it --rm --name dds-clean \
                -v "$(PWD)":/usr/src/mymaven \
                -v "$(HOME)/.m2":/root/.m2  \
                -w /usr/src/mymaven \
                maven:3.8.1-openjdk-17-slim mvn clean

docker:

