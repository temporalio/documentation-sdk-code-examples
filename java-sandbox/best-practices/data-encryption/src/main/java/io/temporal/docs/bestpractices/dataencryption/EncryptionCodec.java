package io.temporal.docs.bestpractices.dataencryption;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.codec.PayloadCodecException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// @@@SNIPSTART java-data-encryption-payload-codec
public class EncryptionCodec implements PayloadCodec {

    static final ByteString METADATA_ENCODING = ByteString.copyFromUtf8("binary/encrypted");

    private static final String CIPHER = "AES/GCM/NoPadding";

    // Metadata keys and values added to each encrypted Payload. The decode method checks them.
    static final String METADATA_ENCRYPTION_CIPHER_KEY = "encryption-cipher";
    static final ByteString METADATA_ENCRYPTION_CIPHER = ByteString.copyFromUtf8(CIPHER);
    static final String METADATA_ENCRYPTION_KEY_ID_KEY = "encryption-key-id";

    private static final int GCM_NONCE_LENGTH_BYTE = 12;
    private static final int GCM_TAG_LENGTH_BIT = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Load the keys from a KMS or other secure storage. The codec records the key ID on each
    // Payload, so after you rotate to a new active key, it can still decrypt older Payloads.
    private final Map<String, SecretKey> keys;
    private final String activeKeyId;

    public EncryptionCodec(Map<String, SecretKey> keys, String activeKeyId) {
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("No key with ID " + activeKeyId);
        }
        this.keys = Map.copyOf(keys);
        this.activeKeyId = activeKeyId;
    }

    @Override
    public List<Payload> encode(List<Payload> payloads) {
        return payloads.stream().map(this::encodePayload).collect(Collectors.toList());
    }

    @Override
    public List<Payload> decode(List<Payload> payloads) {
        return payloads.stream().map(this::decodePayload).collect(Collectors.toList());
    }

    private Payload encodePayload(Payload payload) {
        byte[] encryptedData;
        try {
            // Encrypt the whole Payload, including its metadata.
            encryptedData = encrypt(payload.toByteArray(), keys.get(activeKeyId));
        } catch (Exception e) {
            throw new PayloadCodecException(e);
        }
        return Payload.newBuilder()
            .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, METADATA_ENCODING)
            .putMetadata(METADATA_ENCRYPTION_CIPHER_KEY, METADATA_ENCRYPTION_CIPHER)
            .putMetadata(METADATA_ENCRYPTION_KEY_ID_KEY, ByteString.copyFromUtf8(activeKeyId))
            .setData(ByteString.copyFrom(encryptedData))
            .build();
    }

    private Payload decodePayload(Payload payload) {
        // Leave Payloads this codec didn't encrypt unchanged.
        if (!METADATA_ENCODING.equals(
                payload.getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, null))) {
            return payload;
        }
        try {
            String keyId =
                payload.getMetadataOrThrow(METADATA_ENCRYPTION_KEY_ID_KEY)
                    .toString(StandardCharsets.UTF_8);
            SecretKey key = keys.get(keyId);
            if (key == null) {
                throw new IllegalStateException("No key with ID " + keyId);
            }
            byte[] plainData = decrypt(payload.getData().toByteArray(), key);
            return Payload.parseFrom(plainData);
        } catch (Exception e) {
            throw new PayloadCodecException(e);
        }
    }

    private static byte[] encrypt(byte[] plainData, SecretKey key) throws Exception {
        // Use a new random nonce for every Payload and store it in front of the ciphertext.
        byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTE];
        RANDOM.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce));
        byte[] encryptedData = cipher.doFinal(plainData);

        return ByteBuffer.allocate(nonce.length + encryptedData.length)
            .put(nonce)
            .put(encryptedData)
            .array();
    }

    private static byte[] decrypt(byte[] encryptedDataWithNonce, SecretKey key) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedDataWithNonce);
        byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTE];
        buffer.get(nonce);
        byte[] encryptedData = new byte[buffer.remaining()];
        buffer.get(encryptedData);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce));
        return cipher.doFinal(encryptedData);
    }
}
// @@@SNIPEND
