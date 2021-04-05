OS=$(shell uname -s | tr '[:upper:]' '[:lower:]')
CONTAINER_STRUCTURE_TEST_VERSION=v1.8.0
IMAGE_NAME=sauron-service
COMMIT_ID=$(shell git rev-parse HEAD)
DOCKER_TAG=$(shell git describe --abbrev=0 --tags | cut -d '-' -f 3 )

all: test build_jar build_image

clean: cleanup_container cleanup_jar

test_jar:
	mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent test -U

build_jar:
	mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify org.jacoco:jacoco-maven-plugin:report -U

release_jar:
	mvn release:prepare -B -Darguments="-Dmaven.test.skip=true"
	mvn release:perform -Darguments="-Dmaven.test.skip=true"

cleanup_jar:
	mvn clean

download_container_structure:
	curl --silent -L -o container-structure-test https://storage.googleapis.com/container-structure-test/${CONTAINER_STRUCTURE_TEST_VERSION}/container-structure-test-$(OS)-amd64 && chmod +x container-structure-test

test_container: build_jar download_container_structure
	docker build --pull --no-cache --build-arg "VERSION=containertest" -t ${IMAGE_NAME}:containertest .
	./container-structure-test -v error test --image ${IMAGE_NAME}:containertest --config docker/container-structure-test.yml
	rm container-structure-test

cleanup_container:
	docker image rm openjdk:11-jre-slim || true
	docker image rm ${IMAGE_NAME}:containertest || true
	docker image rm ${IMAGE_NAME}:${COMMIT_ID} || true
	docker image rm ${IMAGE_NAME}:${DOCKER_TAG} || true
	docker image rm ${IMAGE_NAME}:latest || true

test: test_jar test_container

build_image:
	docker build --pull -t ${IMAGE_NAME} .
	docker tag ${IMAGE_NAME} ${IMAGE_NAME}:${COMMIT_ID}

release_image:
	docker tag ${IMAGE_NAME} ${IMAGE_NAME}:${DOCKER_TAG}
