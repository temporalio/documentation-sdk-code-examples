package io.temporal.docs.bestpractices.dataconversion;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.common.converter.PayloadConverter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// @@@SNIPSTART java-data-conversion-custom-payload-converter
/** Payload Converter that writes Money as a JSON string such as "12.34 USD". */
public class MoneyPayloadConverter implements PayloadConverter {
    // This converter replaces the default "json/plain" converter, so it delegates every
    // other value to Jackson.
    private final JacksonJsonPayloadConverter jackson = new JacksonJsonPayloadConverter();

    @Override
    public String getEncodingType() {
        return "json/plain"; // The encoding type determines which default conversion behavior to override.
    }

    @Override
    public Optional<Payload> toData(Object value) throws DataConverterException {
        if (!(value instanceof Money money)) {
            return jackson.toData(value);
        }
        String json = "\"" + money + "\"";
        return Optional.of(
            Payload.newBuilder()
                .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, ByteString.copyFromUtf8(getEncodingType()))
                .setData(ByteString.copyFrom(json, StandardCharsets.UTF_8))
                .build());
    }

    @Override
    public <T> T fromData(Payload content, Class<T> valueClass, Type valueType)
        throws DataConverterException {
        if (valueClass != Money.class) {
            return jackson.fromData(content, valueClass, valueType);
        }
        String text = jackson.fromData(content, String.class, String.class);
        return valueClass.cast(Money.parse(text));
    }
}
// @@@SNIPEND
