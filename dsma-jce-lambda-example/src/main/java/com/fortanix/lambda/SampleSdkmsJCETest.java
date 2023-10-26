package com.fortanix.lambda;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.AlgorithmParameters;
import java.util.Base64;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fortanix.sdkms.jce.provider.service.SdkmsKeyService;
import com.fortanix.sdkms.jce.provider.service.ApiClientSetup;
import com.fortanix.sdkms.jce.provider.keys.sym.aes.SdkmsAESKey;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;
import com.fortanix.sdkms.v1.model.KeyObject;
import java.util.List;

import com.fortanix.sdkms.jce.provider.SdkmsJCE;

public class SampleSdkmsJCETest implements RequestHandler<List<Integer>, Void>{

    private static SdkmsJCE provider;
    public Cipher cipher;
    public SdkmsAESKey aesKey;
    public AlgorithmParameters params;

    private static final String kid = System.getenv("KEY_ID");

    public SampleSdkmsJCETest() {
        System.setProperty("FORTANIX_API_ENDPOINT",  System.getenv("FORTANIX_API_ENDPOINT"));
        System.setProperty("FORTANIX_API_KEY", System.getenv("FORTANIX_API_KEY"));
		System.setProperty("dsm.accelerator", "true");
        provider = SdkmsJCE.getInstance();
        Security.addProvider(provider);
    }

    public void init() {
        try {
            cipher = Cipher.getInstance("AES_256/CBC/PKCS5Padding", provider);
			SobjectDescriptor sobjDesc = new SobjectDescriptor().kid(kid);
			KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
			aesKey = new SdkmsAESKey(keyObj);
            params = cipher.getParameters();
        } catch ( Exception e) {
            e.printStackTrace();
		}
    }

    public String encrypt(String plainText)
            throws IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, params);
        byte[] byteStream = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes = this.cipher.doFinal(byteStream);
        return Base64.getEncoder().encodeToString(cipherBytes);
    }

    public String decrypt(String cipherText)
            throws IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        cipher.init(Cipher.DECRYPT_MODE, aesKey, params);
        byte[] byteStream = Base64.getDecoder().decode(cipherText);
        byte[] plainBytes = this.cipher.doFinal(byteStream);
        return new String(plainBytes);
    }

	@Override
	public Void handleRequest(List<Integer> event, Context context) {
        System.out.println("Installed Java version: "+ System.getProperty("java.version"));

        SampleSdkmsJCETest test = new SampleSdkmsJCETest();

        String plainText = "The quick brown fox jumps over the lazy dog";

        try {
            test.init();

            String cipherText = test.encrypt(plainText);
            System.out.println("cipherText: " + cipherText);

            String revertedText = test.decrypt(cipherText);
            System.out.println("plainText after decryption: " + revertedText);

            if (revertedText.equalsIgnoreCase(plainText)) {
                System.out.println("Matches");
            } else {
                System.out.println("Doesn't Match");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		return null;
    }
}
