.PHONY: \
	build \
	build-yolo \
	compile \
	run \
	run-development \
	package \
	generate-javadoc \
	clean \
	deploy-to-aws \
	create-archetype \
	local-up \
 	local-down \
 	integration-test \
 	unit-test \
 	checkstyle \
 	sonar

build:
	./mvnw verify -P jacoco

build-yolo:
	./mvnw verify -DskipTests

build-local:
	./mvnw verify -P jacoco,localbuild

compile:
	./mvnw compile
  
run:
	./mvnw spring-boot:run

run-development:
	SPRING_PROFILES_ACTIVE=development ./mvnw spring-boot:run

package:
	./mvnw package

unit-test:
	./mvnw clean test -P jacoco

integration-test:
	./mvnw verify -DskipUnitTests -P jacoco

checkstyle:
	./mvnw checkstyle:checkstyle

generate-javadoc:
	./mvnw javadoc:javadoc

clean:
	./mvnw clean

deploy-to-aws:
	test $(S3_BUCKET_NAME)
	test $(STACK_NAME)

	aws cloudformation package \
	--template-file sam.yaml \
	--output-template-file /tmp/output-sam.yaml \
	--s3-bucket $(S3_BUCKET_NAME)

	aws cloudformation deploy \
	--template-file /tmp/output-sam.yaml \
	--stack-name $(STACK_NAME) \
	--capabilities CAPABILITY_IAM

	aws cloudformation describe-stacks --stack-name $(STACK_NAME)

create-archetype: clean
	@if test ! -s ~/.m2/settings.xml; then \
		echo settings.xml does not exist, creating one; \
		echo "<settings></settings>" > ~/.m2/settings.xml; \
	fi
	./mvnw archetype:create-from-project
	./mvnw -f target/generated-sources/archetype/pom.xml install

sonar:
	./mvnw sonar:sonar

local-api:
	docker-compose -f docker/docker-compose-local-api.yml up -d

local-up:
	docker-compose -f docker/docker-compose.yml -p postgres_docker up -d

local-down:
	docker-compose -f docker/docker-compose.yml -p postgres_docker down

local-db-up:
	docker-compose -f docker/docker-compose.yml -p postgres_docker up -d postgres

local-db-down:
	docker-compose -f docker/docker-compose.yml -p postgres_docker down

# Example run: 'make sam-local-run EVENT=src/test/resources/sample_lambda_events/import_10_taxis.json'
sam-local-run:
	SPRING_PROFILES_ACTIVE='sam-local' sam local invoke JaquCazFunction -t sam.yaml -e $$EVENT --docker-network postgres_docker_default

localstack-up:
	SERVICES='s3,sqs,sns' docker-compose -f docker/docker-compose-localstack.yml -p localstack_docker up -d

localstack-down:
	docker-compose -f docker/docker-compose-localstack.yml -p localstack_docker down

local-services-up: local-db-up localstack-up

local-services-down: local-db-down localstack-down

localstack-run:
	SPRING_PROFILES_ACTIVE='localstack,development' AWS_PROFILE='localstack' AWS_REGION='eu-west-2' ./mvnw spring-boot:run

dependency-security-check:
	./mvnw org.owasp:dependency-check-maven:check -P security

local-integration-up:
	docker-compose -f src/it/resources/docker-compose-it.yml up -d

local-integration-down:
	docker-compose -f src/it/resources/docker-compose-it.yml down

