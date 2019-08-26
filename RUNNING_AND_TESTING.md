[Go back to main README](README.md)

## Building
Build and package Lambda application, run all checks and tests, calculate code coverage:
```
$ make clean build
```
If you want to only do local runs you can skip creating Lambda zip file:
```
$ make compile
```
Build the project without unit and integration tests: 
```
$ make build-yolo
```

## Testing

### Automated tests

#### Run whole harness
Run all automated tests:
```
make build
```

#### Unit tests
Run unit tests: 
```
$ make unit-test
``` 
*NOTE*: the project is only compiled

#### Integration tests
Run integration tests: 
```
$ make integration-test
``` 
*NOTE*: <br>
a) unit tests are skipped, <br>
b) project is compiled and packaged alongside with static code analysis

Integration tests are a part of the build process and run once the project has been packaged.
They are located in `src/it` which acts as a test-source directory.

All dependent services are created in Docker containers:
- database using `postgres:11-alpine` image
- AWS using `localstack/localstack` image

See `pom.xml` (`docker-maven-plugin`) for details on docker configuration when running integration tests.

### Manual testing: Alternative 1: Local run as Spring-Boot app with as many AWS services as possible. Recommended.
This is the recommended way to run and test service locally. It is most convenient to use, fastest to 
spin and suitable for 99% cases. The only missing 1% can be tested using Alternative 2 described below but
it should be very rare.
 
#### Prerequisities
You need to have installed:
1. Java 8 runtime
2. Docker
3. make utility (comes as standard on Linux and Mac)

You need to have AWS account with at least programmatic access. And:
1. Make sure you have proper AWS config entry in file in ~/.aws/credentials - there needs to be a profile with your keys.
2. You need to have environment variable set: export AWS_PROFILE=name_of_profile 
3. You need to have environment variable set: export AWS_REGION=eu-west-2 (you can choose whatever region works for you)

#### Building
1. Pull the branch you want from https://github.com/InformedSolutions/JAQU-CAZ-Retrofit-API
2. Run `make clean` to make sure that everything is clean and ready to build.
3. Run `make build` to build and test project locally - it may take some time as there are a lot of tests including integration ones.

#### Postgres
This backend component requires Postgres to connect to. If you already have Postgres running on localhost:5432 you don't have do to anything. If not, you can easily run our Docker container:
`make local-db-up` - it will run fresh Postgres and expose it on localhost:5432.

#### Run service locally
If you want to run application locally, as regular Spring-Boot application you need to specify
Spring profile `development`. In this profile service will not use any AWS Lambda related parts or
switch them to the ones suitable for such local run. However in this mode service will still
use all other AWS services that are publicly available, for example S3. 
For it to work you need to have correctly configured AWS account and access with `~/.aws/credentials` 
file and AWS profile specified in `AWS_PROFILE` env variable and AWS Region specified in `AWS_REGION`. 
And now run:
```
$ make run-development
```

Now you can call Spring-Boot endpoints as usual using REST client for example curl or Postman.

### Manual testing: Alternative 2: AWS Lambda Integration type run and test
This mode is suitable if we want to test AWS Lambda to Spring-Boot plumbing code. See
`StreamLambdaHandler` or `RetrofitRegisterCsvFromS3Lambda` classes to see how this plumbing looks like.
For any other functionality Alternative 1 is recommended.

In this mode we will run and test our Lambda code in simulated local Lambda runner. 
It can be achieved with support of AWS SAM Local CLI tool. In short, this tool uses Docker to
provide similar runtime to the one AWS uses to run our Lambdas on production.
Moreover, in this mode we will try to use as much real AWS services as possible. For example we
will connect to real S3 buckets and use real SQS and SNS services.
We must somehow mock RDS/Postgres as the one on AWS is not accessible. For that we use Docker container.

#### Prerequisities
1. Have AWS CLI tool installed. See official [AWS Guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
2. Have AWS SAM Local CLI tool installed. See official [AWS Guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
3. Make sure you have properly configured AWS credentials and profile in `~/.aws/credentials`. You must have your profile added with access key id and secret access key.
4. Make sure you have **AWS_PROFILE** and **AWS_REGION** environment variables set and exported (and matching your profile from credentials file). SAM Local will pass those credentials automatically to Docker with Lambda runtime.

#### Running and testing 
1. Run `make local-db-up` to spin up local Postgres Docker container. It will automatically set proper network and name.
2. Rebuild and repackage your project: `make build`.  You will have to do it every time you make any changes.
3. Run `make sam-local-run EVENT=path_to_json_file_with_lambda_event` - this uses SAM Local tool to run specified Lambda function locally and passing input JSON event into it.
Beware as first run will download Docker images which can take a while.
4. In case of any changes just repackage as described in point 2 and then rerun as in 3.
5. After you're done, drop local Postgres by `make local-db-down` and you are in a clean state.
6. Final note: JSON file with Lambda event is quite complicated and is something that API Gateway sends to Lambda. It must 
contain all HTTP request details, optionally security context as well.

### Manual testing: Alternative 3: Mocked AWS Services, lightweight local run and test
This mode should not be used manually in general. We use LocalStack in Integration Tests to make
them independent from real AWS services and predictable. However in rare cases, before creating
Integration Test we might want to pre-test something manually. And this Alternative can then be handy.

In this mode we will run our app as regular Spring-Boot application, just like in Alternative 1, so without any Lambda 
wrappers and Lambda entrypoints. Spring-Boot controller exposes REST endpoints which we can
use from curl or Postman to test functionality. LocalStack will be used as mock for AWS services,
including S3, SQS, SNS and more. 
We must somehow mock RDS/Postgres. For that we use Docker container.

It is important to create dummy AWS profile in your `~/.aws/credentials` file.
```
[localstack]
aws_access_key_id = dummy
aws_secret_access_key = dummy
```
Our run configuration will expect this profile to be set up. Access key and secret key can be set
to `dummy` to make sure we won't use any real AWS services.

#### Prerequisities
1. Have AWS CLI tool installed. See official [AWS Guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
2. `[localstack]` profile in `~/.aws/credentials` file.
3. Docker installed.

#### Running and testing 
1. Run `make local-db-up` to spin up local Postgres Docker container if your services requires Postgres.
2. Run `make localstack-up` to spin up Localstack Docker container with mocked AWS services.
3. If you need Postgres you can combine steps 1 and 2 into one and run: `make local-services-up`
to spin Postgres and Localstack using one command.
4. Check `application-localstack.yml` configuration file, Spring profile `localstack` - it overrides AWS service
endpoints to use localhost. (you don't have to change the defaults if you haven't changed Localstack config)
5. Run app by `make localstack-run`. It will set proper env vars and run Spring-Boot app as usual.
6. You can test endpoints using curl or Postman as usual.
7. After you're done, drop local Postgres and Localstack: `make local-services-down` or 
`make local-db-down` and `make localstack-down` if you want to run 2 separate commands.

#### Preparing S3 data
Some helper AWS commands to prepare data on S3:
1. `aws --endpoint http://localhost:4572 s3 mb s3://ntr-data` - creates S3 bucket `ntr-data`
2. `aws --endpoint http://localhost:4572 s3 cp ~/Downloads/records-10.csv s3://ntr-data` - 
puts `records-10.csv` file from `~/Downloads` into `ntr-data` bucket.
3. `aws --endpoint-url=http://localhost:4572 s3api put-bucket-acl --bucket ntr-data --acl public-read` - 
makes bucket publicly available.

Then you can use S3Client to get this object programmatically using Java.

