## DSMA JCE lambda Example

This template example serves as a starting point for applications trying to use DSMA JCE in a lambda function.
Maven is used to build and package a shaded JAR which is supposed to be deployed on Lambda.

### Steps to build and package
To make a shaded JAR (JAR with dependencies) make sure that you have all the dependencies present in your local system.
You can get the latest DSMA JCE jar from [here](https://support.fortanix.com/hc/en-us/articles/12717106726804-DSM-Accelerator-JCE-Provider).
Ensure that JCE bundled JAR is installed locally. You can do this by:
```shell
mvn install:install-file -Dfile=./sdkms-jce-provider-bundled-dsma-4.XX.XXXX.jar -DgroupId=com.fortanix -DartifactId=sdkms-jce-provider-dsma -Dversion=4.XX.XXXX -Dpackaging=jar...
```
Note: Change the version number in pom.xml as per the installation.

With the jce dependency installed we can now run the following command to package and build the program
```shell
mvn clean package shade:shade -DskipTest
```

### Lambda function Management

you can refer to this section in [aws docs](https://docs.aws.amazon.com/lambda/latest/dg/java-package.html#java-package-console) for deployment steps

Or use the following command to create a lambda function

```shell
aws lambda create-function --function-name <lambda_function_name> \
--runtime java11 --handler com.fortanix.lambda.SampleSdkmsJCETest::handleRequest \
--role <ROLE> \
--zip-file fileb://path/to/shaded/jar
```
and to update lambda function
```shell
aws lambda update-function-code --function-name <lambda_function_name> \
--zip-file fileb://path/to/shaded/jar
```

### Cofigurations

Once the lambda function is created ensure the following:
- Memory should be at least 512 MB
- timeout should be at least 15 s

Refer this [doc](https://docs.aws.amazon.com/lambda/latest/dg/configuration-function-common.html#configuration-memory-console) for configuring the above listed parameters

Now, we have to set environment variables for the lambda function:
- Set `FORTANIX_API_ENDPOINT` to any dsm endpoint
- Set `FORTANIX_API_KEY` to appropriate Credential
- Set `FORTANIX_DSM_ACCELERATOR` to `true`
- Set `KEY_ID` to an existing key in the account. This is needed for the example as we will be using the same to perform cryptography

### Testing

Now that the lambda function is configured, we can test it from the test tab in console. 
Since the example takes an Integer as a trigger event, you can send any integer to trigger the preset crypto operations.