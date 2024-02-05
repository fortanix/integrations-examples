package com.fortanix.constants;

/* This enum contains various transformations which can be used when creating
 *  a Cihper instance for crypto operations.
 * */
public enum CipherTransformations {

    // AES Cipher
    AES_128_CBC_PKCS5PADDING("AES_128/CBC/PKCS5PADDING"),

    // DES Cipher
    DES_56_CBC_PKCS5PADDING("DES/CBC/PKCS5PADDING"),

    // DES3 Cipher (a.k.a TripleDES)
    DES3_168_CBC_PKCS5PADDING("TripleDES/CBC/PKCS5PADDING"),

    //RSA Cipher
    RSA("RSA");

    private String mode;

    CipherTransformations(String mode) {
        this.mode = mode;
    }

    public String mode() {
        return this.mode;
    }

}
