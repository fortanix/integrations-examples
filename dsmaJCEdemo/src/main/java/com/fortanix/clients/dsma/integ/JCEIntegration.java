package com.fortanix.clients.dsma.integ;

import com.fortanix.sdkms.jce.provider.SdkmsJCE;
import com.fortanix.sdkms.jce.provider.keys.sym.aes.SdkmsAESKey;
import com.fortanix.sdkms.jce.provider.service.SdkmsKeyService;
import com.fortanix.sdkms.v1.model.KeyObject;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;

public class JCEIntegration {

    // replace with dsm url for env you are pointing to.
    private String apiEndpoint = "<Endpoint URL>";

    // replace with your api-key
    private String apiKey = "<API Key>";

    private static SdkmsJCE provider;

    public JCEIntegration() {
        provider = SdkmsJCE.initialize(apiEndpoint, apiKey);
        Security.addProvider(provider);
    }

    /* Create a cipher based on a valid transformation for the object-type.
    *
    * */
    public Pair createCipher(String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance(transformation, provider);
        AlgorithmParameters params = cipher.getParameters();
        Pair<Cipher, AlgorithmParameters> pair = new MutablePair<>(cipher, params);
        return pair;
    }

    /* Initialize the cipher for encrypt call and perform encryption. */
    public String encrypt(String plainText, Key key, Pair<Cipher, AlgorithmParameters> cipherPair) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = cipherPair.getLeft();
        AlgorithmParameters params = cipherPair.getRight();

        cipher.init(Cipher.ENCRYPT_MODE, key, params);

        byte[] byteStream = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes = cipher.doFinal(byteStream);
        return Base64.getEncoder().encodeToString(cipherBytes);
    }

    /* Initialize the cipher for decrypt call and perform decryption. */
    public String decrypt(String cipherText, Key key, Pair<Cipher, AlgorithmParameters> cipherPair) throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = cipherPair.getLeft();
        AlgorithmParameters params = cipherPair.getRight();

        cipher.init(Cipher.DECRYPT_MODE, key, params);

        byte[] byteStream = Base64.getDecoder().decode(cipherText);
        byte[] plainBytes = cipher.doFinal(byteStream);
        return new String(plainBytes);
    }

    // recommended way for dsma-flow
    /* In this approach, we create a simple keyObject directly knowing the key_id and/or key_name
     * here, key_size is important to set because cipher validates the input key's size against
     * it's expected size. This approach will NOT require an extra call to DSM to fetch any key-info
     * and proceeds with encryption. In dsma mode, the encrypt call will move to dsma lib.
    */
    public Key basicKeyObject(String kid, String keyName, int keySize) {
        KeyObject keyObj = new KeyObject();
        keyObj.setKid(kid);
        keyObj.setName(keyName);
        keyObj.keySize(keySize);

        return new SdkmsAESKey(keyObj);
    }

    /* This approach can be used to fetch key-info when we know only key_id and/or key_name
    *  and require more key-info for creating the key and cipher instances.
    *  This makes a call to DSM's GET key-info API /crypto/v1/keys/{key-id} that responds only
    *  with key-metadata (NOT key-material) which can be used to create a key and cipher instance
    *  and proceed with encryption.
    *  Note: key-metadata fetched through this approach is only for application logic using dsma-jce
    *  and is not cached within dsma.
    *  When the encrypt call is invoked, for dsma mode - the control flow will move to dsma lib which will
    *  make an export call to fetch key-material and cache it for subsequent calls as well before
    *  performing encryption locally.
    * */
    public Key fetchKeyMetadataById(String kid) {
        SobjectDescriptor sobjDesc = new SobjectDescriptor().kid(kid);
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        return new SdkmsAESKey(keyObj);
    }

    /* Same as above, only uses key_name instead of key_id to fetch the key-metadata info
    *  from DSM's GET key-info API /crypto/v1/keys/{key-id}
    * */
    public Key fetchKeyMetadataByName(String keyName) {
        SobjectDescriptor sobjDesc = new SobjectDescriptor().name(keyName);
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        return new SdkmsAESKey(keyObj);
    }


    /* This approach uses keyStore to fetch key-metadata from DSM using the object alias or key_name.
    * */
    public Key fetchKeyMetadataFromKeyStore(String keyName) throws CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("SDKMS", provider);
        ks.load(null, null);
        Key aesKey = ks.getKey(keyName, null);
        return aesKey;
    }

}
