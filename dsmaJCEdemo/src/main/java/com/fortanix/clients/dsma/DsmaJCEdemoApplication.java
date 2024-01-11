package com.fortanix.clients.dsma;

import com.fortanix.clients.dsma.constants.CipherTransformations;
import com.fortanix.clients.dsma.integ.JCEIntegration;
import com.fortanix.sdkms.jce.provider.SdkmsJCE;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Key;
import java.security.Security;

@SpringBootApplication
public class DsmaJCEdemoApplication {

	public static JCEIntegration integration = new JCEIntegration();

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DsmaJCEdemoApplication.class, args);

		// display system info
		System.out.println("Installed Java version: " + System.getProperty("java.version"));

		String plainText1 = "The quick brown fox jumps over the lazy dog";
		String plainText2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit";
		String plainText3 = "5105105105105100";

		// AES encrypt/decrypt use-cases
		String aes_key_id = "<AES Key UUID>";
		String aes_key_name = "<AES Key Name>";

		/* here, we show that the same aes key can be instantiated and/or fetched in different ways.
		 * aeskey1 is same as aeskey2 and aeskey3
		 * */
		Key aeskey1 = integration.basicKeyObject(aes_key_id, aes_key_name, 256);
		doEncryptDecrypt(plainText1, aeskey1, CipherTransformations.AES_256_CBC_NOPADDING.mode());

		Key aeskey2 = integration.fetchKeyMetadataById(aes_key_id);
		doEncryptDecrypt(plainText2, aeskey2, CipherTransformations.AES_256_CBC_PKCS5PADDING.mode());

		Key aeskey3 = integration.fetchKeyMetadataFromKeyStore(aes_key_name);
		doEncryptDecrypt(plainText3, aeskey3, CipherTransformations.AES_256_ECB_PKCS5PADDING.mode());

		// AES FPE tokenization/detokenization use-cases
		String token_key_id = "<Tokenization Key UUID>";		//256
		// AES_256_FPE_NOPADDING
		String token_key_name = "<Tokenization Key Name>";
		String credit_card_num = "5200828282828210";

		Key fpekey1 = integration.basicKeyObject(token_key_id, token_key_name, 256);
		doEncryptDecrypt(credit_card_num, fpekey1, CipherTransformations.AES_256_FPE_NOPADDING.mode());

		Key fpekey2 = integration.fetchKeyMetadataByName(token_key_name);
		doEncryptDecrypt(credit_card_num, fpekey2, CipherTransformations.AES_256_FPE_NOPADDING.mode());

		Key fpekey3 = integration.fetchKeyMetadataFromKeyStore(token_key_name);
		doEncryptDecrypt(credit_card_num, fpekey3, CipherTransformations.AES_256_FPE_NOPADDING.mode());

		// DES(56bit) encrypt/decrypt use-cases
		String des_key_id = "<DES Key UUID>";
		String des_key_name = "<DES Key Name>";

		Key deskey1 = integration.basicKeyObject(des_key_id, des_key_name, 56);
		doEncryptDecrypt(plainText1, deskey1, CipherTransformations.DES_56_CBC_NOPADDING.mode());

		Key deskey2 = integration.fetchKeyMetadataByName(des_key_name);
		doEncryptDecrypt(plainText2, deskey2, CipherTransformations.DES_56_CBC_PKCS5PADDING.mode());

		Key deskey3 = integration.fetchKeyMetadataFromKeyStore(des_key_name);
		doEncryptDecrypt(plainText3, deskey3, CipherTransformations.DES_56_ECB_NOPADDING.mode());

		// DES3 encrypt/decrypt use-cases
		String des3_key_id = "<DES3 Key UUID>";
		String des3_key_name = "<DES3 Key Name>";

		Key des3key1 = integration.basicKeyObject(des3_key_id, des3_key_name, 168);
		doEncryptDecrypt(plainText1, des3key1, CipherTransformations.DES3_168_CBC_NOPADDING.mode());

		Key des3key2 = integration.fetchKeyMetadataById(des3_key_id);
		doEncryptDecrypt(plainText2, des3key2, CipherTransformations.DES3_168_ECB_NOPADDING.mode());

		Key des3key3 = integration.fetchKeyMetadataFromKeyStore(des3_key_name);
		doEncryptDecrypt(plainText3, des3key3, CipherTransformations.DES3_168_CBC_PKCS5PADDING.mode());
	}

	/* Here, we create a cipher instance for the required transformation such as AES_128, DES, DES3 with supported
	*  paddings and use the cipher to perform encrypt and decrypt on data.
	* */
	public static void doEncryptDecrypt(String plainText, Key aesKey, String transformation) {
		String encryptedData = null, decryptedData = null;

		try {
			Pair cipherPair = integration.createCipher(transformation);

			encryptedData = integration.encrypt(plainText, aesKey, cipherPair);
			decryptedData = integration.decrypt(encryptedData, aesKey, cipherPair);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		System.out.println(plainText);
		System.out.println(encryptedData);
		System.out.println(decryptedData);

		if (decryptedData.equalsIgnoreCase(plainText)) {
			System.out.println("Matches");
		} else {
			System.out.println("Doesn't Match");
		}
	}
}
