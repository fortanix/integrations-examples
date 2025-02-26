
# Fortanix MongoDB CSFLE  

This project demonstrates how to use **Client-Side Field Level Encryption (CSFLE)** with MongoDB, leveraging Fortanix Data Security Manager (DSM) as a Key Management Interoperability Protocol (KMIP) provider. It includes schema encryption, data key generation, and secure data operations implemented in Java.

---

## Prerequisites
1. **Java Development Kit (JDK)** - Version 8 or higher.
2. **Maven** - For building the project.
3. **MongoDB Server** - Installed and running locally or remotely.
4. **Fortanix DSM Endpoint** - Accessible KMIP endpoint.
5. **Fortanix DSM Configuration**
   1. DSM Group
   2. DSM cert based App
   3. DSM Plugin to create or rotate a 96 byte secret (Find the code for the lua plugin at `plugins/plugin.lua`)
---

## Configuration

### Setting Up the TrustStore and KeyStore

To configure the `trustStore` and `keyStore` in the `FortanixMongodbCSFLE.java` file, follow these steps:

1. **TrustStore**: This is the path to the root certificate authority (CA) file in JKS (Java KeyStore) format, which is used to validate the DSM endpoint.

   - **Path**: `/path/to/rootCA.jks`
   - **Password**: `trustStorePassword`

2. **KeyStore**: This is the path to your PKCS#12 (.p12) file, which contains both the private key and the signed certificate used to authenticate with your DSM application.

   - **Path**: `/path/to/keystore.p12`
   - **Password**: `keyStorePassword`

---

## Steps in FortanixMongodbCSFLE.java

### Setup KMS Provider:
Configure Fortanix DSM as the KMIP provider.

### Generate a Master Key
Invoke the DSM Plugin to create 96 byte secret in DSM that will act as the Master Key.

### Generate a Data Encryption Key (DEK):
Generate a DEK using ClientEncryptionSettings.
Save the DEK ID as a Base64-encoded string for schema validation.

### Define JSON Schema:
Specify which fields need encryption and their encryption algorithms.
Use AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic for fields requiring deterministic encryption.
Use AEAD_AES_256_CBC_HMAC_SHA_512-Random for fields requiring random encryption.

### Configure MongoDB Client:
Create two MongoDB clients:
A secure client with CSFLE enabled.

### Insert Encrypted Data:
Insert a sample document using the secure client.

### Query the Data:
Retrieve and print the inserted document using secure client.

### Clean Up:
Close the MongoDB client to release resources.

---

## Steps in RotateCMK.java

### Setup KMS Provider:
Configure Fortanix DSM as the KMIP provider.

### Rotate the CMK
Invoke the plugin to rotate the CMK to a new 96 byte secret.

### Rewrap DEK
Use the new CMK to rewrap the DEKs.
