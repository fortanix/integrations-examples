import com.fortanix.sdkms.jce.provider.SdkmsJCE;
import com.fortanix.sdkms.jce.provider.keys.asym.rsa.RSAPrivateKeyImpl;
import com.fortanix.sdkms.jce.provider.keys.asym.rsa.RSAPublicKeyImpl;
import com.fortanix.sdkms.jce.provider.constants.ProviderConstants;
import com.fortanix.sdkms.jce.provider.keys.sym.aes.SdkmsAESKey;
import com.fortanix.sdkms.jce.provider.service.SdkmsKeyService;
import com.fortanix.sdkms.v1.model.KeyObject;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;
import static com.fortanix.sdkms.jce.provider.constants.ProviderConstants.*;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.Objects;


public class DSMAJCE {
    // replace with dsm url for env you are pointing to.
    private static String FORTANIX_API_ENDPOINT = "<Endpoint URL>";

    // replace with your api-key
    private static String FORTANIX_API_KEY = "<API Key>";
    // replace with your app uuid. Only required for cert-based auth
    private static String FORTANIX_APP_UUID = "<APP UUID>";
    // AES key UUID used for encrypt/decrypt examples
    private static String AES_KEY_UUID = "<AES_KEY_UUID>";
    // Name of the Tokenization key of Credit card type used for tokenize/detokenize examples
    private static String TOKEN_KEY_NAME = "<TOKEN_KEY_NAME>";
    // Name of the RSA  key used for sign/verify examples
    private static String RSA_KEY_NAME = "<RSA_KEY_NAME>";

    // for config-file or env variable usage
    private static SdkmsJCE provider1;
    //api-key auth
    private static SdkmsJCE provider2;
    //cert-based auth
    private static SdkmsJCE provider3;
    // plain text used for encrypt/decrypt
    private static String PLAIN = "the small brown fox jumps over the lazy dog";
    // Credit card sample text used for tokenize/detokenize
    private static String Credit_card = "5200828282828210";
    private static byte[] iv = new byte[16];
    private static final String AAD = "TestAAD";

    private static byte[] encrypt(Provider provider, String mode, String padding, SecretKey key, String PLAIN) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException {
        String algorithm = String.format("AES_%d/%s/%s", 256, mode, padding);
        Cipher cipher = Cipher.getInstance(algorithm, provider);
        AlgorithmParameters params;
        if(Objects.equals(mode, GCM)){
            iv = new byte[ProviderConstants.GCM_IV_LEN];
            int tagLen = 128;
            params = AlgorithmParameters.getInstance("GCM");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(tagLen, iv);
            params.init(gcmSpec);
            cipher.init(Cipher.ENCRYPT_MODE, key, params);
            cipher.updateAAD(AAD.getBytes());
        } else
            if (Objects.equals(mode, CTR)) {
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        } else{
            params = cipher.getParameters();
            cipher.init(Cipher.ENCRYPT_MODE, key, params);
        }
        System.out.println("Plain text: " + PLAIN);
        // Encrypted content
        return cipher.doFinal(PLAIN.getBytes());
    }

    private static byte[] decrypt(Provider provider, String mode, String padding, SecretKey key, byte[] cipherBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException {
        String algorithm = String.format("AES_%d/%s/%s", 256, mode, padding);
        Cipher cipher = Cipher.getInstance(algorithm, provider);
        AlgorithmParameters params;
        if(Objects.equals(mode, GCM)){
            iv = new byte[ProviderConstants.GCM_IV_LEN];
            int tagLen = 128;
            params = AlgorithmParameters.getInstance("GCM");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(tagLen, iv);
            params.init(gcmSpec);
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            cipher.updateAAD(AAD.getBytes());
        } else
            if (Objects.equals(mode, CTR)) {
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        } else{
            params = cipher.getParameters();
            cipher.init(Cipher.DECRYPT_MODE, key, params);
        }
        // decrypt the same content
        return cipher.doFinal(cipherBytes);
    }

    private static byte[] tokenization(Provider provider, String mode, String padding, SecretKey key, String PLAIN) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String algorithm = String.format("AES_%d/%s/%s", 256, mode, padding);
        Cipher cipher = Cipher.getInstance(algorithm, provider);
        AlgorithmParameters params = cipher.getParameters();
        cipher.init(Cipher.ENCRYPT_MODE, key, params);
        System.out.println("Plain text: " + PLAIN);
        // Encrypted content
        return cipher.doFinal(PLAIN.getBytes());
    }
    private static byte[] detokenization(Provider provider, String mode, String padding, SecretKey key, byte[] cipherBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String algorithm = String.format("AES_%d/%s/%s", 256, mode, padding);
        Cipher cipher = Cipher.getInstance(algorithm, provider);
        AlgorithmParameters params = cipher.getParameters();
        // decrypt the same content
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        return cipher.doFinal(cipherBytes);
    }
    public static byte[] sign(Provider provider, String algorithm, RSAPrivateKeyImpl rsaPrivateKey, String pssParameterDigest, MGF1ParameterSpec mgf1ParameterSpec) throws UnsupportedEncodingException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] data = "test".getBytes("UTF8");
        Signature sig = Signature.getInstance(algorithm, provider);
        if (mgf1ParameterSpec != null || pssParameterDigest != null) {
            sig.setParameter(new PSSParameterSpec(pssParameterDigest, "MGF1", mgf1ParameterSpec, 32, 1));
        }
        sig.initSign(rsaPrivateKey);
        sig.update(data);
        return sig.sign();
    }
    public static boolean verify(Provider provider, String algorithm, RSAPublicKeyImpl rsaPublicKey, byte[] signatureBytes, String pssParameterDigest, MGF1ParameterSpec mgf1ParameterSpec) throws UnsupportedEncodingException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] data = "test".getBytes("UTF8");
        Signature sig = Signature.getInstance(algorithm, provider);
        if (mgf1ParameterSpec != null || pssParameterDigest != null) {
            sig.setParameter(new PSSParameterSpec(pssParameterDigest, "MGF1", mgf1ParameterSpec, 32, 1));
        }
        sig.initVerify(rsaPublicKey);
        sig.update(ByteBuffer.wrap(data));
        return sig.verify(signatureBytes);
    }

    public static boolean aes_test(Provider provider, String mode, String padding) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException {
        // Here we're querying the key using the key UUID from Fortanix DSM. Make sure the key has "export" permission for DSMA use-case.
        SobjectDescriptor sobjDesc = new SobjectDescriptor().kid(AES_KEY_UUID);
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        SdkmsAESKey key = new SdkmsAESKey(keyObj);
        // encrypt the content
        byte[] ciphertext = encrypt(provider, mode, padding, key, PLAIN);
        if(ciphertext == null)return false;
        // decrypt the same content
        byte[] plaintext = decrypt(provider, mode, padding, key, ciphertext);
        if (plaintext == null)return false;
        return Arrays.equals(PLAIN.getBytes(), plaintext);
    }

    public static boolean fpe_test(Provider provider) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException {
        // Here we're querying the key using the key name from Fortanix DSM. Make sure the key has "export" permission for DSMA use-case.
        /* For this particular example we're using tokenization key of type credit card.
         * To make this example work change <Key Name> with name of tokenization key of type
         * credit card. If you're using different type of key modify "PLAIN" accordingly.
         */
        SobjectDescriptor sobjDesc = new SobjectDescriptor().name(TOKEN_KEY_NAME);
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        SdkmsAESKey key = new SdkmsAESKey(keyObj);
        // tokenization of credit card number
        byte[] ciphertext = tokenization(provider, FPE, NOPADDING, key, Credit_card);
        if(ciphertext==null)return false;
        // detokenization of credit card number
        byte[] plaintext = detokenization(provider, FPE, NOPADDING, key, ciphertext);
        if (plaintext == null)return false;
        return Arrays.equals(Credit_card.getBytes(), plaintext);
    }

    public static boolean rsa_test(Provider provider, String algorithm, String pssParameterDigest, MGF1ParameterSpec mgf1ParameterSpec) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException, UnsupportedEncodingException, SignatureException {
        // Here we're querying the key using the key name from Fortanix DSM. Make sure the key has "export" permission for DSMA use-case.
        SobjectDescriptor sobjDesc = new SobjectDescriptor().name(RSA_KEY_NAME);
        KeyObject keyObj = SdkmsKeyService.getKeyObject(sobjDesc);
        RSAPublicKeyImpl rsaPublicKey = new RSAPublicKeyImpl(keyObj);
        RSAPrivateKeyImpl rsaPrivateKey = new RSAPrivateKeyImpl(keyObj);
        // signing the data
        byte[] signatureBytes = sign(provider, algorithm, rsaPrivateKey, pssParameterDigest, mgf1ParameterSpec);
        if(signatureBytes==null)return false;
        // verifying the data
        return verify(provider, algorithm, rsaPublicKey, signatureBytes, pssParameterDigest, mgf1ParameterSpec);
    }

    public static void main(String[] args) throws Exception {
        // To use the cert-based authentication in DSMA JCE, configure the following Java properties:
        /* The key pair is stored in a file referred to as a keystore. To access this keystore, the
        * JDK's SSL provider relies on specific system properties, namely javax.net.ssl.keyStore and
        * javax.net.ssl.keyStorePassword. Additionally, the property javax.net.ssl.keyStoreType defines
        * the type of keystore in use.
        * */
        System.setProperty("javax.net.ssl.keyStoreType", "jks");
        System.setProperty("javax.net.ssl.keyStore", "<pkcs12_file_path>");
        System.setProperty("javax.net.ssl.keyStorePassword", "<keystore_password>");

        // NOTE: Use any one of the following provider examples & comment or remove others to make this example work.
        // for config-file or env variable usage
        provider1 = SdkmsJCE.getInstance();
        Security.addProvider(provider1);

        //api-key auth
        provider2 = SdkmsJCE.initialize(FORTANIX_API_ENDPOINT, FORTANIX_API_KEY);
        Security.addProvider(provider2);

        // replace with your app uuid. Only required for cert-based auth
        // configure properties "javax.net.ssl.keyStore" & "javax.net.ssl.keyStorePassword" above to use cert-based authentication
        provider3 = SdkmsJCE.initialize(FORTANIX_API_ENDPOINT, FORTANIX_APP_UUID);
        Security.addProvider(provider3);

        // tests CBC mode with system generated IV
        aes_test(provider1, CBC, NOPADDING);
        // tests CTR mode with user provided IV
        aes_test(provider1, CTR, PKCS5PADDING);
        // tests GCM mode with user provided IV
        aes_test(provider1, GCM, PKCS5PADDING);
        // tokenization
        fpe_test(provider1);
        // sign/verify examples using RSA key
        rsa_test(provider1, "SHA256withRSA", null, null);
        rsa_test(provider1, "SHA256withRSAandPKCSV1_5", null, null);
        rsa_test(provider1, "SHA512withRSAandMGF1", "SHA512", MGF1ParameterSpec.SHA512);

    }
}
