package com.fortanix.integ;

import com.fortanix.sdkms.jce.provider.SdkmsJCE;
import com.fortanix.sdkms.jce.provider.spec.SecurityObjectParameterSpec;
import com.fortanix.sdkms.v1.model.KeyOperations;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.Arrays;

public class JCEIntegration {

    // replace with dsm url for env you are pointing to.
    private String apiEndpoint = "<Endpoint URL>";

    // replace with your api-key
    private String apiKey = "<API Key>";

    public static SdkmsJCE provider;

    public JCEIntegration() {
        provider = SdkmsJCE.initialize(apiEndpoint, apiKey);
        Security.addProvider(provider);
    }

    public SecretKey generateKey(String algorithm, int size) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm, provider);
        SecurityObjectParameterSpec params = new SecurityObjectParameterSpec(Arrays.asList(KeyOperations.ENCRYPT, KeyOperations.DECRYPT, KeyOperations.EXPORT, KeyOperations.WRAPKEY, KeyOperations.UNWRAPKEY), false);
        keyGenerator.init(size);
        keyGenerator.init(params);
        return keyGenerator.generateKey();
    }

    public KeyPair generateRsaKey(int size) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        // Call goes to RSAKeyPairGenerator.java
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA", provider);
        SecurityObjectParameterSpec params = new SecurityObjectParameterSpec(Arrays.asList(KeyOperations.ENCRYPT, KeyOperations.DECRYPT, KeyOperations.EXPORT, KeyOperations.WRAPKEY, KeyOperations.UNWRAPKEY), false);
        keyGenerator.initialize(size);
        keyGenerator.initialize(params);
        return keyGenerator.generateKeyPair();
    }

}
