package com.dbops.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesEncryptor {

    private final SecretKeySpec keySpec;

    public AesEncryptor(@Value("${dbops.encrypt-key}") String key) {
        if (key.length() != 32) {
            throw new IllegalArgumentException("encrypt-key는 32자(AES-256)여야 합니다.");
        }
        this.keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    }

    public String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encoded)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("복호화 실패", e);
        }
    }
}