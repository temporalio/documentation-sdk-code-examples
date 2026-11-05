package io.temporal.docs.bestpractices.dataconversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;

public class Converters {

    // @@@SNIPSTART java-data-conversion-custom-jackson
    public static JacksonJsonPayloadConverter yourCustomJacksonJsonPayloadConverter() {
        // Start from the SDK's default ObjectMapper so Java time types, Optional, and
        // field visibility keep working, then add your changes.
        ObjectMapper objectMapper = JacksonJsonPayloadConverter.newDefaultObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JacksonJsonPayloadConverter(objectMapper);
    }
    // @@@SNIPEND

    public static WorkflowClientOptions clientOptions() {
        // @@@SNIPSTART java-data-conversion-set-converter
        DefaultDataConverter ddc =
            DefaultDataConverter.newDefaultInstance()
                .withPayloadConverterOverrides(new MoneyPayloadConverter());

        WorkflowClientOptions workflowClientOptions =
            WorkflowClientOptions.newBuilder().setDataConverter(ddc).build();
        // @@@SNIPEND
        return workflowClientOptions;
    }
}
