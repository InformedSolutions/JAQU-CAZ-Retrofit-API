# JAQU-CAZ-Retrofit Register API
JAQU CAZ Retrofit Register API

## Features

#### Validation

Validation rules can be found [here](#validation-rules-for-retrofitted-vehicles).

## Development Environment Configuration

### Vagrant
A Vagrant development machine definition inclusive of the following software assets can be found at the root of this repository:

1. Ubuntu 18.04 LTS
1. Eclipse for Java Enterprise
1. OpenJDK 8
1. Maven
1. Git
1. Docker CE (for backing tools used for example to emulate AWS lambda functions and DB instances)

As a complimentary note, this Vagrant image targets VirtualBox as its provider. As such, the necessary technical dependencies installed on the host are simply VirtualBox and Vagrant.

## Local Development: building, running and testing

[Detailed descripton of how to build, run and test Retrofit service](RUNNING_AND_TESTING.md)

## Database management

Liquibase is being used as database migrations tool.
Please check `src/main/resources/db.changelog` directory. It contains file named `db.changelog-master.yaml`
which is automatically picked up by Spring Boot at application startup. This file drives
application of all changesets and migrations.

### Liquibase naming convention
Each changeset should be prefixed with consecutive 4-digit number left padded with zeros.
For example: 0001, 0002, 0003. Then current application version should be put and finally some
short description of change. For example:

`0001-1.0-create_tables_taxi_phv_licensing_authority.yaml`

What we see is application order number, at what application version change was made and finally
a short description of changes. Pretty informative and clean.

If one changeset file contains more than one change, please put consecutive numbers in changeset id:

`0002.1`, `0002.2` and so on.

Raw SQL files must be used from Liquibase Yaml changesets and put into `rawSql` subfolder.
Please use existing files as an example.

## API specification

API specification is available at `{server.host}:{server.port}/v1/swagger-docs` (locally usually at http://localhost:8080/v1/swagger-docs)

## Commit hooks

To minimize the risk of making a _broken_ commit you may want to enable a git pre-commit hook which 
builds the project before a change is committed. Please execute the following in the root project 
directory:
```
$ developer-resources/scripts/git-hooks/install-pre-commit-hook.sh
```
This will create a symlink to `developer-resources/scripts/git-hooks/pre-commit-hook.sh`. If 
the symlink exists or there is another `pre-commit` file in `.git/hooks` directory, the script does 
nothing and appropriate error message is displayed.

### Yolo mode

If you want to disable the hook please use `--no-verify` option for `git commit`.

## Branching Strategy

### GitFlow

We use [GitFlow](https://nvie.com/posts/a-successful-git-branching-model/) for source control management.

At a high level, GitFlow consists of the following workflow:

-   New features are developed in a `feature` branch
-   `feature` branches are created off the `develop` branch and merged back into it when ready to be released
-   A `release` branch is created from the `develop` branch when it is time to release
-   The `release` branch is used to track prospective deployments - any fixes made prior to a release should target this branch
-   After the release, the `release` branch is merged into both `master` and `develop`, at which point the release is tagged in `master`
-   `hotfix` branches are created from `master` and merged back into `master` and `develop` when finished

### Branch naming

Branch names should follow the following pattern:

```
[branchType]/[ticketReference]/[description]
```
For example:
```
feature/NTR-9/my-small-feature
```
### Branch types

|Branch Type     |Use case                       |
|----------|-------------------------------|
|`feature` |New feature for the user, not a new feature for build script|
|`fix`     |Bug fix for the user, not a fix to a build script|
|`refactor`|Refactoring production code, e.g. renaming a variable|
|`docs`    |Changes to the documentation|
|`style`   |Formatting, missing semi colons, etc.; no production code change|
|`test`    |Adding missing tests, refactoring tests; no production code change|
|`chore`   |Updating build scripts etc.; no production code change|

### Merging into protected branches

The `develop`  and `master` branches are protected branches. Polices are enforced to prevent unstable code escalating into a Production environment. In order to merge into these branches, the following conditions must be satisfied:

- [ ] proposed code changes has been submitted for, and accepted following, peer review
- [ ] CI tests have passed

## Validation rules for registering retrofitted vehicles

| Rule description                                                                             | Error message                                                                                             |
|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------| 
| Missing VRM field                                                                            | N/A - there will be a parse error.                                                                        | 
| Empty VRM field                                                                              | Line {}: Vrm should have from 1 to 7 characters instead of 0.                                             | 
| VRM field too long                                                                           | Line {}: Vrm should have from 1 to 7 characters instead of 28.                                            | 
| Invalid format of VRM                                                                        | Line {}: Invalid format of VRM (regex validation).                                                        | 
| Missing licence start/end date                                                               | N/A - there will be a parse error                                                                         | 
| Invalid format of licence start/end date                                                     | Line {}: Invalid format of licence {START_OR_END} date, should be ISO 8601.                               | 
| Missing licence type                                                                         | N/A - there will be a parse error.                                                                        | 
| Invalid format of licence type                                                               | Line {}: Invalid value of 'taxiOrPHV' field. Allowable values: [taxi, PHV].                               | 
| Missing licence plate number                                                                 | N/A - there will be a parse error                                                                         | 
| Invalid format of licence plate number                                                       | Line {}: 'LicensePlateNumber' should have from 1 to 15 characters instead of {ACTUAL_LENGTH}.             | 
| Missing licensing authority name                                                             | N/A - there will be a parse error                                                                         | 
| Invalid format of licensing authority name                                                   | Line {}: 'licensingAuthorityName' should have from 1 to 50 characters instead of {ACTUAL_LENGTH}.         | 
| Cannot connect to S3 bucket/filename. Bucket or filename does not exist or is not accessible.| S3 bucket or file not found or not accessible                                                             | 
| Lack of 'uploader-id' metadata                                                               | uploader-id' not found in file's metadata                                                                 | 
| Invalid format of 'uploader-id'                                                              | Malformed ID of an entity which want to register vehicles by CSV file. Expected a unique identifier (UUID)| 
| Too large CSV file                                                                           | Uploaded file is too large. Maximum allowed: 104857600 bytes                                              | 
| Invalid fields number in CSV                                                                 | Line {}: Line contains invalid number of fields (actual value: {}, allowable values: 6 or 7).             | 
| Maximum line length exceeded                                                                 | Line {}: Line is too long (actual value: {}, allowed value: 110).                                         | 
| Invalid format of a line (e.g. it contains invalid characters)                               | Line {}: Line contains invalid character(s), is empty or has trailing comma character.                    | 
| Potentially included header row                                                              | Line 1: {VALIDATION_ERROR_MSG}. Please make sure you have not included a header row.                      | 
| Potentially included trailing row                                                            | Line {LAST_LINE_NO}: {VALIDATION_ERROR_MSG}. Please make sure you have not included a trailing row.       | 

### Validation rules applicable only to REST API dealing with starting/querying import jobs 

| Rule description                                       | Error message                                                                                                                                                                                                      |
|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Missing request payload                                | ```{"vrm":"","title":"Validation error","detail":"Required request body is missing(...)","status":400}```                                                                                                          |
| Empty JSON payload ('{}')                              | ```{"vrm":"","title":"Validation error","detail":"vehicleDetails cannot be null","status":400}```                                                                                                                  |
| Malformed request payload (malformed JSON, e.g. '{,}') | ```{"vrm":"","title":"Validation error","detail":"JSON parse error: Unexpected character ...","status":400}```                                                                                                     |
| Missing Content-type header                            | ```{"timestamp":1565101505257,"status":415,"error":"Unsupported Media Type","message":"Content type  not supported","path":"/v1/scheme-management/taxiphvdatabase","code":"UNKNOWN"}```                            |
| Unsupported Content-type header                        | ```{"timestamp":1565101610979,"status":415,"error":"Unsupported Media Type","message":"Content type {NOT_SUPPORTED_CONTENT_TYPE} not supported","path":"/v1/scheme-management/taxiphvdatabase","code":"UNKNOWN"}```|
| Wrong HTTP method                                      | ```{"timestamp":1565101671868,"status":405,"error":"Method Not Allowed","message":"Request method {METHOD} not supported","path":"/v1/scheme-management/taxiphvdatabase","code":"CAZ00070"}```                     | 
| Missing 'X-Correlation-ID' header                      | ```Missing request header 'X-Correlation-ID' for method parameter of type String```                                                                                                                                | 
| Missing 'x-api-key' header                             | ```Missing request header 'x-api-key' for method parameter of type String```                                                                                                                                       | 
