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
import java.util.*;

public class RotateCMK {
    public static final String DEK_BASE64_ID = "hGVMX7+wS92/VArfdEhEYw==";
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

//    Copy the UUID of the plugin that was created in DSM to create/rotate a 96 byte secret
    public static final String PLUGIN_UUID = "<PLUGIN ID IN DSM>";// Ex: "0XXXXXXX-YYYY-HHHH-GGGG-123456789123";
    public static final String PLUGIN_API = "/sys/v1/plugins/";
//  How to identify the hostname for cert based authentication to invoke the DSM plugin? (FORTANIX_API_ENDPOINT)
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

    private static String fetchCMKFromDEKId() {
        Binary uuidBinary = convertUUIDToBinary(getUUID(RotateCMK.DEK_BASE64_ID));
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
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FORTANIX_API_ENDPOINT + PLUGIN_API + PLUGIN_UUID))
                .header("Content-Type", "application/json")
                .header("Authorization", AUTH_HEADER)
                .POST(HttpRequest.BodyPublishers.ofString("{\"kid\":\"" + oldCMKId + "\",\"method\":\"rotate\"}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JSONObject responseObject = new JSONObject(response.body());
            System.out.println("CMK successfully rotated. New Master Key UUID: " + responseObject.getString("kid"));
            return responseObject.getString("kid");
        } else {
            System.out.println("Request failed with status code: " + response.statusCode() + "\nError: " + response.body());
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
        // configure SSL properties for keyStore and trustStore
        configureSSLProperties();
        // fetch the existing CMK ID
        String oldCMKId = fetchCMKFromDEKId();
        // rotate the existing CMK in DSM
        String newCMKId = sendRotateRequest(oldCMKId);
        // rewrap the DEK with the new CMK
        rewrapDataKeys(newCMKId);
    }
}