package com.itbm.inf2fm.kinetix.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Converter
public class CriptografiaDadosConverter implements AttributeConverter<String, String> {
    private static final String ENV_KEY = "KINETIX_DATA_ENCRYPTION_KEY";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] payload = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível criptografar um dado clínico.", exception);
        }
    }

    @Override
    public String convertToEntityAttribute(String payload) {
        if (payload == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            if (bytes.length <= NONCE_LENGTH) throw new IllegalArgumentException("Payload clínico inválido.");
            byte[] nonce = Arrays.copyOfRange(bytes, 0, NONCE_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(bytes, NONCE_LENGTH, bytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível descriptografar um dado clínico. Confira a chave KINETIX_DATA_ENCRYPTION_KEY.", exception);
        }
    }

    private static SecretKeySpec key() {
        String encoded = System.getenv(ENV_KEY);
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Configure KINETIX_DATA_ENCRYPTION_KEY antes de acessar dados clínicos.");
        }
        byte[] decoded = Base64.getDecoder().decode(encoded);
        if (decoded.length != 32) throw new IllegalStateException("KINETIX_DATA_ENCRYPTION_KEY deve decodificar para 32 bytes.");
        return new SecretKeySpec(decoded, "AES");
    }
}
