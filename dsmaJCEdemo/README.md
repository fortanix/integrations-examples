![fortanix-logo](images/fortanix-logo.png)

# DSMA JCE Example Project

## Project Description
This is a sample Spring Boot(3.1.3) project built with Maven, supporting Java 17, and designed to be seamlessly run on any integrated development environment (IDE). This project includes example codes of performing crypto operations from Fortanix Data Security Manager(DSM).

## Getting Started

To get started with this project, follow these steps:

1. Clone the repository to your local machine.

```bash
git clone https://github.com/fortanix/integrations-examples.git
```
2. Import the project into your favorite IDE. For example, in IntelliJ IDEA, go to "File" -> "Open" and select the project's root directory(dsmaJCEdemo).

## To run

1. Download DSMA JCE jar from https://support.fortanix.com/hc/en-us/articles/12717106726804-DSM-Accelerator-JCE-Provider

2. In pom.xml of this project replace `${dsma-jce.version}` & `${basedir}/path/to/jarfile/sdkms-jce-provider-bundled-dsma-${dsma-jce.version}.jar` with version & path of DSMA JCE you have downloaded.
```
<dependency>
    <!-- jar dependency provided as system with path in local directory -->
    <groupId>com.fortanix</groupId>
    <artifactId>sdkms-jce-provider-dsma</artifactId>
    <version>${dsma-jce.version}</version>
    <systemPath>${basedir}/path/to/jarfile/sdkms-jce-provider-bundled-dsma-${dsma-jce.version}.jar</systemPath>
    <scope>system</scope>
</dependency>
```

3. Replace `<Endpoint URL>` in JCEIntegration.java with the relevant dsm instance you are pointing to.
4. Replace `<API Key>`in JCEIntegration.java with your api-key.

![Configuration](images/Configuration.png)

5. Replace all the `<Key UUID>` & `<Key Name>` with your security object's Key UUID & Key Name in DsmaJCEdemoApplication.java

![Key-config](images/Key-config.png)

6. Run DsmaJCEdemoApplication class as main java application.