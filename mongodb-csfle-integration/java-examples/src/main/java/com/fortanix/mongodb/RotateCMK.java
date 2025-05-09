package com.fortanix.mongodb;

import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.vault.RewrapManyDataKeyOptions;
import com.mongodb.client.model.vault.RewrapManyDataKeyResult;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

public class RotateCMK {
    public static final String TRUSTSTORE_TYPE = "jks";
    public static final String TRUSTSTORE_PATH = ""; // Ex: "/path/to/trustStore.jks"
    public static final String TRUSTSTORE_PASSWORD = ""; // Ex: "trustStorePassword"
    public static final String KEYSTORE_TYPE = "pkcs12";
    public static final String KEYSTORE_PATH = ""; // Ex: "/path/to/keyStore.p12"
    public static final String KEYSTORE_PASSWORD = ""; // Ex: "keyStorePassword"

    public static final String MONGO_CONNECTION_STRING = "mongodb://localhost:27017";
    public static final String KEY_VAULT_DB_NAME = "encryption";
    public static final String KEY_VAULT_COLLECTION_NAME = "__keyVault";
//    DSM endpoint for KMIP to connect to DSM
    public static final String KMS_PROVIDER = "kmip";
    public static final String KMS_ENDPOINT = "<DSM ENDPOINT URL>"; // Ex: "https://apac.smartkey.io"

//  How to identify the hostname for cert based authentication to DSM
//  1. Login as an administrator to DSM
//  2. Go to Settings
//  3. Go to Interfaces
//  4. Identify the hostname where Request client certificate has been checked.
    public static final String FORTANIX_API_ENDPOINT = "<DSM ENDPOINT URL FOR CERT-BASED AUTHENTICATION>"; // Ex: "https://api.apac/smartkey.io"
//    base64 encoding of {"<certificate_app_ID>" + ":"}
//    EG: Base64(0XXXXXXX-ABCD-HHHH-GGGG-123456789123:)
    public static final String AUTH_HEADER = "<Basic Base64{App_ID:}>" ; // Ex: "Basic MFhYWFhYWFgtWVlZWS1ISEhILUdHR0ctMTIzNDU2Nzg5MTIzOgo=";


    private static void configureSSLProperties() {
        System.setProperty("javax.net.ssl.trustStoreType", TRUSTSTORE_TYPE);
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);
        System. setProperty("javax.net.ssl.keyStoreType", KEYSTORE_TYPE);
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
        System. setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);
    }

    private static String fetchCMKFromDEKId(String dekBase64Id) {
        Binary uuidBinary = convertUUIDToBinary(getUUID(dekBase64Id));
        MongoClient mongoClient = MongoClients.create(MONGO_CONNECTION_STRING);
        MongoDatabase database = mongoClient.getDatabase(KEY_VAULT_DB_NAME);
        MongoCollection<Document> collection = database.getCollection(KEY_VAULT_COLLECTION_NAME);
        Document document = (Document) Objects.requireNonNull(collection.find(Filters.eq("_id", uuidBinary)).first()).get("masterKey");
        mongoClient.close();
        return (String) document.get("keyId");
    }

    private static Binary convertUUIDToBinary(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return new Binary((byte) 4, byteBuffer.array());
    }

    public static UUID getUUID(String base64keyID){
        byte[] decodedBytes = Base64.getDecoder().decode(base64keyID);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
        long mostSignificantBits = byteBuffer.getLong();
        long leastSignificantBits = byteBuffer.getLong();
        UUID uuid = new UUID(mostSignificantBits, leastSignificantBits);
        System.out.println("DEK ID: " + uuid);
        return uuid;
    }

    private static String sendRotateRequest(String oldCMKId) throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        // Generate 96 random bytes
        byte[] secret = new byte[96];
        secureRandom.nextBytes(secret);
        String base64Secret = Base64.getEncoder().encodeToString(secret);
        // Build HTTP client to communicate with DSM API
        HttpClient client = HttpClient.newBuilder().build();

        // GET Sobject Request
        HttpRequest getOldCMKData = HttpRequest.newBuilder()
                .uri(URI.create(FORTANIX_API_ENDPOINT + "/crypto/v1/keys/" + oldCMKId))
                .header("Content-Type", "application/json")
                .header("Authorization", AUTH_HEADER)
                .GET().build();
        HttpResponse<String> oldCMKDataResponse = client.send(getOldCMKData, HttpResponse.BodyHandlers.ofString());
        String oldCmkName = null;
        if (oldCMKDataResponse.statusCode() == 200) {
            JSONObject oldCmkData = new JSONObject(oldCMKDataResponse.body());
            oldCmkName = oldCmkData.getString("name");
        } else {
            System.out.println("Request failed with status code: " + oldCMKDataResponse.statusCode() + "\nError: " + oldCMKDataResponse.body());
            return null;
        }
        // POST Rotate Sobject Request
        HttpRequest postRekeyRequest = HttpRequest.newBuilder()
                .uri(URI.create(FORTANIX_API_ENDPOINT + "/crypto/v1/keys/rekey"))
                .header("Content-Type", "application/json")
                .header("Authorization", AUTH_HEADER)
                .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                        "    \"value\": \"" + base64Secret + "\",\n" +
                        "    \"name\": \"" + oldCmkName + "\"\n" +
                        "}"))
                .build();
        HttpResponse<String> rekeyResponse = client.send(postRekeyRequest, HttpResponse.BodyHandlers.ofString());
        if (rekeyResponse.statusCode() == 201) {
            JSONObject newCMKData = new JSONObject(rekeyResponse.body());
            System.out.println("CMK successfully rotated. New Master Key UUID: " + newCMKData.getString("kid"));
            return newCMKData.getString("kid");
        } else {
            System.out.println("Request failed with status code: " + rekeyResponse.statusCode() + "\nError: " + rekeyResponse.body());
            return null;
        }
    }

    private static void rewrapDataKeys(String newCMKId) {
        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        Map<String, Object> providerDetails = new HashMap<>();
        providerDetails.put("endpoint", KMS_ENDPOINT);
        kmsProviders.put(KMS_PROVIDER, providerDetails);

        ClientEncryptionSettings clientEncryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(MONGO_CONNECTION_STRING))
                        .build())
                .keyVaultNamespace(KEY_VAULT_DB_NAME + "." + KEY_VAULT_COLLECTION_NAME)
                .kmsProviders(kmsProviders)
                .build();

        try (MongoClient mongoClient = MongoClients.create(MONGO_CONNECTION_STRING);
            ClientEncryption clientEncryption = ClientEncryptions.create(clientEncryptionSettings)) {
            Bson filter = new Document();
            Document masterKeyProperties = new Document().append("keyId", newCMKId);
            RewrapManyDataKeyOptions rewrapManyDataKeyOptions = new RewrapManyDataKeyOptions()
                    .masterKey(masterKeyProperties.toBsonDocument())
                    .provider(KMS_PROVIDER);
            RewrapManyDataKeyResult result = clientEncryption.rewrapManyDataKey(filter, rewrapManyDataKeyOptions);
            System.out.println("Rewrapped Data Keys: " + result.getBulkWriteResult().getModifiedCount());
        }
    }


    public static void main(String[] args) throws Exception {
        String dekBase64Id = System.getProperty("kid");
        if (dekBase64Id == null || dekBase64Id.isEmpty()) {
            System.out.println("Usage: java -Dkid=<base64_key_id> -cp target/classes com.fortanix.mongodb.RotateCMK");
            System.out.println("Example: java -Dkid=FlH02YLXXXXXXXXXXXXX== -cp target/classes com.fortanix.mongodb.RotateCMK");
            System.exit(1);
        }
        System.out.println("Using provided DEK ID: " + dekBase64Id);
        // configure SSL properties for keyStore and trustStore
        configureSSLProperties();
        // fetch the existing CMK ID
        String oldCMKId = fetchCMKFromDEKId(dekBase64Id);
        // rotate the existing CMK in DSM
        String newCMKId = sendRotateRequest(oldCMKId);
        // rewrap the DEK with the new CMK
        rewrapDataKeys(newCMKId);
    }
}
