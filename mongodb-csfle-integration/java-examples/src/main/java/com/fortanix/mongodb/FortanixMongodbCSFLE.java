package com.fortanix.mongodb;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.bson.Document;
import org.json.JSONObject;


public class FortanixMongodbCSFLE {
    public static final String TRUSTSTORE_TYPE = "jks";
    public static final String TRUSTSTORE_PATH = ""; // Ex: "/path/to/trustStore.jks"
    public static final String TRUSTSTORE_PASSWORD = ""; // Ex: "trustStorePassword"
    public static final String KEYSTORE_TYPE = "pkcs12";
    public static final String KEYSTORE_PATH = ""; // Ex: "/path/to/keyStore.p12"
    public static final String KEYSTORE_PASSWORD = ""; // Ex: "keyStorePassword"

    public static final String MONGO_CONNECTION_STRING = "mongodb://localhost:27017";
    public static final String KEY_VAULT_NAMESPACE = "encryption.__keyVault";
    public static final String KMS_PROVIDER = "kmip";
    public static final String ENCRYPTION_ALGORITHM = "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic";
//    DSM endpoint for KMIP to connect to DSM
    public static final String KMS_ENDPOINT = "<DSM ENDPOINT URL>"; // Ex: "https://apac.smartkey.io"
//    Constants for Sample document
    public static final String EMPLOYEE_NAME = "Alice";
    public static final String DB_NAME = "testFortanix";
    public static final String COLLECTION_NAME = "employees";

    private static void configureSSLProperties() {
        System.setProperty("javax.net.ssl.trustStoreType", TRUSTSTORE_TYPE);
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.keyStoreType", KEYSTORE_TYPE);
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);
    }

    private static String createCMKandDEK(Map<String, Map<String, Object>> kmsProviders) throws IOException, InterruptedException {
        /// DEK CREATION
        ClientEncryptionSettings clientEncryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
                        .build())
                .keyVaultNamespace(KEY_VAULT_NAMESPACE)
                .kmsProviders(kmsProviders)
                .build();
        ClientEncryption clientEncryption = ClientEncryptions.create(clientEncryptionSettings);
        Document masterKeyProperties = new Document();
        // If an existing security object in DSM needs to be used as CMK then it must be of size 96 bytes (secret or HMAC) and it must have export permission.
        // keyId is the UUID of the security object in DSM.
        // Then, define your masterKeyProperties like this:
        // Document masterKeyProperties = new Document().append("keyId", keyId);

        BsonBinary dataKeyId = clientEncryption.createDataKey(KMS_PROVIDER, new DataKeyOptions().masterKey(masterKeyProperties.toBsonDocument()));
        String base64DataKeyId = Base64.getEncoder().encodeToString(dataKeyId.getData());
        System.out.println("Base64 Encoded Key ID of DEK: " + base64DataKeyId);
        clientEncryption.close();
        return base64DataKeyId;
    }

    private static void secureInsert(Map<String, Map<String, Object>> kmsProviders, HashMap<String, BsonDocument> schemaMap){
        // client settings builder with KMIP
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
                .autoEncryptionSettings(AutoEncryptionSettings.builder()
                        .keyVaultNamespace(KEY_VAULT_NAMESPACE)
                        .kmsProviders(kmsProviders)
                        .schemaMap(schemaMap)
//                        .extraOptions(extraOptions)
                        .build())
                .build();

        MongoClient mongoClientSecure = MongoClients.create(clientSettings);
        // Insert the document
        Document employee = new Document()
                .append("name", EMPLOYEE_NAME)
                .append("employeeID", 12345)
                .append("city", "Bengaluru");
        InsertOneResult insertOneResult = mongoClientSecure.getDatabase(DB_NAME).getCollection(COLLECTION_NAME).insertOne(employee);

        // Querying using regular and secure clients to validate CSFLE
        // client settings builder without KMIP
        MongoClientSettings clientSettingsRegular = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
                .build();
        MongoClient mongoClientRegular = MongoClients.create(clientSettingsRegular);
        System.out.println("Finding a document with regular client");
        Document docRegular = mongoClientRegular.getDatabase(DB_NAME).getCollection(COLLECTION_NAME).find(eq("name", EMPLOYEE_NAME)).first();
        System.out.println(docRegular.toJson());
        System.out.println("Finding a document with encrypted client");
        Document docSecure = mongoClientSecure.getDatabase(DB_NAME).getCollection(COLLECTION_NAME).find(eq("name", EMPLOYEE_NAME)).first();
        System.out.println(docSecure.toJson());

        // Close the clients
        mongoClientRegular.close();
        mongoClientSecure.close();
    }

    private static Map<String, Map<String, Object>> setKmsProvier(){
        Map<String, Map<String, Object>> kmsProviders = new HashMap<String, Map<String, Object>>();
        Map<String, Object> providerDetails = new HashMap<>();
        providerDetails.put("endpoint", KMS_ENDPOINT);
        kmsProviders.put(KMS_PROVIDER, providerDetails);
        return kmsProviders;
    }

    private static HashMap<String, BsonDocument> setupSchema(String base64DataKeyId){
        String ENCRYPTED_FIELD_1 = "employeeID";
        String ENCRYPTED_FIELD_2 = "city"
        Document jsonSchema = new Document().append("bsonType", "object").append("encryptMetadata",
                        new Document().append("keyId", new ArrayList<>((Arrays.asList(new Document().append("$binary", new Document()
                                .append("base64", base64DataKeyId)
                                .append("subType", "04")))))))
                .append("properties", new Document()
                        .append(ENCRYPTED_FIELD_1, new Document().append("encrypt", new Document()
                                .append("bsonType", "int")
                                .append("algorithm", ENCRYPTION_ALGORITHM )))
                        .append(ENCRYPTED_FIELD_2, new Document().append("encrypt", new Document()
                                .append("bsonType", "string")
                                .append("algorithm", ENCRYPTION_ALGORITHM))));
        HashMap<String, BsonDocument> schemaMap = new HashMap<String, BsonDocument>();
        schemaMap.put(DB_NAME + "." + COLLECTION_NAME, BsonDocument.parse(jsonSchema.toJson()));
        return schemaMap;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // configure SSL properties for keyStore and trustStore
        configureSSLProperties();
        // providing the KMS details
        Map<String, Map<String, Object>> kmsProviders = setKmsProvier();
        // create CMK in DSM and DEK in MongoDB Key vault and obtain the DEK base64 ID
        String base64DataKeyId = createCMKandDEK(kmsProviders);
        // create a schema that encrypts the fields "employeeID" and "city"
        HashMap<String, BsonDocument> schemaMap = setupSchema(base64DataKeyId);
        // insert the document with CSFLE
        secureInsert(kmsProviders, schemaMap);
    }
}
