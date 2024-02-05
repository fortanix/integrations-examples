package com.fortanix;

import com.fortanix.constants.CipherTransformations;
import com.fortanix.integ.JCEIntegration;
import static com.fortanix.integ.JCEIntegration.provider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class JCEdemo {
    public static JCEIntegration jceIntegration = new JCEIntegration();

    public static void main(String[] args) throws Exception {

        // Generate RSA key to wrap
        PrivateKey keyToWrap = jceIntegration.generateRsaKey(1024).getPrivate(); // this privateKey is just SDKMS private key id

        // Generate AES key for wrapping
        SecretKey wrappingKey_aes = jceIntegration.generateKey("AES", 128);
        wrapunwrap(CipherTransformations.AES_128_CBC_PKCS5PADDING.mode(), wrappingKey_aes, keyToWrap, "RSA");

        wrapunwrap_asym(CipherTransformations.RSA.mode(), keyToWrap, wrappingKey_aes, "AES");

        // Generate DES key for wrapping
        SecretKey wrappingKey_des = jceIntegration.generateKey("DES", 56);
        wrapunwrap(CipherTransformations.DES_56_CBC_PKCS5PADDING.mode(), wrappingKey_des, keyToWrap, "RSA");

        // Generate DES3 key for wrapping
        SecretKey wrappingKey_des3 = jceIntegration.generateKey("TripleDES", 168);
        wrapunwrap(CipherTransformations.DES3_168_CBC_PKCS5PADDING.mode(), wrappingKey_des3, keyToWrap, "RSA");

    }
    public static void wrapunwrap(String transformation, SecretKey wrappingKey, PrivateKey keyToWrap, String wrappedkeyalg) throws NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] wrappedKey = null;
        Key unwrappedKey = null;
        try{
            Cipher cipher = Cipher.getInstance(transformation, provider);
            AlgorithmParameters params = cipher.getParameters();
            // Initialize cipher
            cipher.init(Cipher.WRAP_MODE, wrappingKey, params);
            // Call wrap method with key to be wrapped
            wrappedKey = cipher.wrap(keyToWrap);
            // Initialized cipher for unwrap
            cipher.init(Cipher.UNWRAP_MODE, wrappingKey, params);
            unwrappedKey = cipher.unwrap(wrappedKey, wrappedkeyalg, Cipher.PRIVATE_KEY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotEquals(wrappedKey, null);
        assertNotEquals(unwrappedKey, null);
    }

    public static void wrapunwrap_asym(String transformation, PrivateKey wrappingKey, SecretKey keyToWrap, String wrappedkeyalg) throws NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] wrappedKey = null;
        Key unwrappedKey = null;
        try{
            Cipher cipher = Cipher.getInstance(transformation, provider);
            AlgorithmParameters params = cipher.getParameters();
            // Initialize cipher
            cipher.init(Cipher.WRAP_MODE, wrappingKey, params);
            // Call wrap method with key to be wrapped
            wrappedKey = cipher.wrap(keyToWrap);
            // Initialized cipher for unwrap
            cipher.init(Cipher.UNWRAP_MODE, wrappingKey, params);
            unwrappedKey = cipher.unwrap(wrappedKey, wrappedkeyalg, Cipher.SECRET_KEY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotEquals(wrappedKey, null);
        assertNotEquals(unwrappedKey, null);
    }
}
