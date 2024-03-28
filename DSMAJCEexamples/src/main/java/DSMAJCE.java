import com.fortanix.sdkms.jce.provider.SdkmsJCE;
import com.fortanix.sdkms.jce.provider.keys.asym.rsa.RSAPrivateKeyImpl;
import com.fortanix.sdkms.jce.provider.keys.asym.rsa.RSAPublicKeyImpl;
import com.fortanix.sdkms.jce.provider.keys.sym.aes.SdkmsAESKey;
import com.fortanix.sdkms.jce.provider.service.SdkmsKeyService;
import com.fortanix.sdkms.v1.model.KeyObject;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;
import static com.fortanix.sdkms.jce.provider.constants.ProviderConstants.*;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class DSMAJCE {
    // replace with dsm url for env you are pointing to.
    private static String FORTANIX_API_ENDPOINT = "<Endpoint URL>";

    // replace with your api-key
    private static String FORTANIX_API_KEY = "<API Key>";
    // replace with your app uuid. Only required for cert-based auth
    private static String FORTANIX_APP_UUID = "<APP UUID>";

    // for config-file or env variable usage
    private static SdkmsJCE provider;
    //api-key auth
    private static SdkmsJCE provider1;
    //cert-based auth
    private static SdkmsJCE provider2;

    private static boolean encrypt_decrypt() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String algorithm = String.format("AES_256/CBC/NOPADDING", 256, CBC, NOPADDING);
        Cipher cipher = Cipher.getInstance(algorithm, provider);
        SobjectDescriptor sobjDesc = new SobjectDescriptor().kid("<Key UUID>");
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        SdkmsAESKey key = new SdkmsAESKey(keyObj);
        AlgorithmParameters params = cipher.getParameters();
        cipher.init(Cipher.ENCRYPT_MODE, key, params);
        String PLAIN = "the small brown fox jumps over the lazy dog";;
        System.out.println("Plain text: " + PLAIN);
        // Encrypted content
        byte[] cipherBytes = cipher.doFinal(PLAIN.getBytes());
        System.out.println("CBC mode | encrypted to " + new String(cipherBytes));
        // decrypt the same content
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        System.out.println("CBC mode | decrypted to " + new String(plainBytes));
        return Arrays.equals(PLAIN.getBytes(), plainBytes);
    }

    private static boolean tokenization() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String algorithm = String.format("AES_256/FPE/NOPADDING", 256, FPE, NOPADDING);
        Cipher cipher = Cipher.getInstance(algorithm, provider1);
        SobjectDescriptor sobjDesc = new SobjectDescriptor().name("<Key Name>");
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        SdkmsAESKey key = new SdkmsAESKey(keyObj);
        AlgorithmParameters params = cipher.getParameters();
        cipher.init(Cipher.ENCRYPT_MODE, key, params);
        String PLAIN = "5200828282828210";
        System.out.println("Plain text: " + PLAIN);
        // Encrypted content
        byte[] cipherBytes = cipher.doFinal(PLAIN.getBytes());
        System.out.println("FPE mode | tokenized to " + new String(cipherBytes));
        // decrypt the same content
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        System.out.println("FPE mode | detokenized to " + new String(plainBytes));
        return Arrays.equals(PLAIN.getBytes(), plainBytes);
    }
    public static void sign_verify(String algorithm, int keySize, String pssParameterDigest, MGF1ParameterSpec mgf1ParameterSpec) throws UnsupportedEncodingException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        SobjectDescriptor sobjDesc = new SobjectDescriptor().name("<Key Name>");
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        RSAPublicKeyImpl rsaPublicKey = new RSAPublicKeyImpl(keyObj);
        RSAPrivateKeyImpl rsaPrivateKey = new RSAPrivateKeyImpl(keyObj);
        byte[] data = "test".getBytes("UTF8");
        Signature sig = Signature.getInstance("SHA256withRSA", provider2);
        if (mgf1ParameterSpec != null || pssParameterDigest != null) {
            sig.setParameter(new PSSParameterSpec(pssParameterDigest, "MGF1", mgf1ParameterSpec, 32, 1));
        }
        sig.initSign(rsaPrivateKey);
        sig.update(data);
        byte[] signatureBytes = sig.sign();

        assertNotNull(signatureBytes);

        sig.initVerify(rsaPublicKey);
        sig.update(ByteBuffer.wrap(data));

        assertNotNull(sig);
        assertEquals(true, sig.verify(signatureBytes));
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.ssl.keyStoreType", "jks");
        System.setProperty("javax.net.ssl.keyStore", "<pkcs12_file_path>");
        System.setProperty("javax.net.ssl.keyStorePassword", "<keystore_password>");

        provider = SdkmsJCE.getInstance();
        Security.addProvider(provider);

        provider1 = SdkmsJCE.initialize(FORTANIX_API_ENDPOINT, FORTANIX_API_KEY);
        Security.addProvider(provider1);

        provider2 = SdkmsJCE.initialize(FORTANIX_API_ENDPOINT, FORTANIX_APP_UUID);
        Security.addProvider(provider2);

        encrypt_decrypt();
        tokenization();
        sign_verify("SHA256withRSA", 1024, null, null);
        sign_verify("SHA512withRSAandMGF1", 2048, "SHA512", MGF1ParameterSpec.SHA512);

    }
}
