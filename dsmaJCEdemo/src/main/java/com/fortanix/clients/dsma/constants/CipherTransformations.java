package com.fortanix.clients.dsma.constants;

/* This enum contains various transformations which can be used when creating
*  a Cihper instance for crypto operations.
* */
public enum CipherTransformations {

    // AES Cipher
    AES_128_CBC_NOPADDING("AES_128/CBC/NoPadding"),
    AES_128_CBC_PKCS5PADDING("AES_128/CBC/PKCS5PADDING"),
    AES_128_ECB_PKCS5PADDING("AES_128/ECB/PKCS5PADDING"),

    AES_256_CBC_NOPADDING("AES_256/CBC/NoPadding"),
    AES_256_CBC_PKCS5PADDING("AES_256/CBC/PKCS5PADDING"),
    AES_256_ECB_PKCS5PADDING("AES_256/ECB/PKCS5PADDING"),

    // FPE AES
    AES_128_FPE_NOPADDING("AES_128/FPE/NOPADDING"),
    AES_256_FPE_NOPADDING("AES_256/FPE/NOPADDING"),

    // DES Cipher
    DES_56_CBC_NOPADDING("DES/CBC/NoPadding"),
    DES_56_CBC_PKCS5PADDING("DES/CBC/PKCS5PADDING"),
    DES_56_ECB_NOPADDING("DES/ECB/NoPadding"),
    DES_56_ECB_PKCS5PADDING("DES/ECB/PKCS5PADDING"),

    // DES3 Cipher (a.k.a TripleDES)
    DES3_168_CBC_NOPADDING("TripleDES/CBC/NoPadding"),
    DES3_168_CBC_PKCS5PADDING("TripleDES/CBC/PKCS5PADDING"),
    DES3_168_ECB_NOPADDING("TripleDES/ECB/NoPadding"),
    DES3_168_ECB_PKCS5PADDING("TripleDES/ECB/PKCS5PADDING"),

    // RSA sign-verify
    SHA_256_RSA("SHA256withRSA"),
    SHA_512_RSA("SHA512withRSA");

    private String mode;

    CipherTransformations(String mode) {
        this.mode = mode;
    }

    public String mode() {
        return this.mode;
    }
}
