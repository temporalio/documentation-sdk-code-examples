package io.temporal.docs.bestpractices.dataconversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.enums.v1.EventType;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.common.converter.PayloadConverter;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class DataConversionTest {

    private static final Money PRICE = new Money(new BigDecimal("12.34"), "USD");

    public static class Order {
        public String orderId;
        public Instant placedAt;

        public Order() {}

        public Order(String orderId, Instant placedAt) {
            this.orderId = orderId;
            this.placedAt = placedAt;
        }
    }

    private static String data(Payload payload) {
        return payload.getData().toStringUtf8();
    }

    private static String encoding(Payload payload) {
        return payload.getMetadataOrThrow(EncodingKeys.METADATA_ENCODING_KEY).toStringUtf8();
    }

    @Test
    public void defaultConverterCannotReadMoneyBack() {
        DataConverter converter = DefaultDataConverter.newDefaultInstance();
        Payload payload = converter.toPayload(PRICE).orElseThrow();
        assertThrows(DataConverterException.class,
            () -> converter.fromPayload(payload, Money.class, Money.class));
    }

    @Test
    public void customConverterWritesMoneyAndDelegatesEverythingElse() {
        DataConverter converter = Converters.clientOptions().getDataConverter();

        Payload money = converter.toPayload(PRICE).orElseThrow();
        assertEquals("json/plain", encoding(money));
        assertEquals("\"12.34 USD\"", data(money));
        assertEquals("12.34 USD", converter.fromPayload(money, Money.class, Money.class).toString());

        Payload text = converter.toPayload("EU").orElseThrow();
        assertEquals("\"EU\"", data(text));
        assertEquals("EU", converter.fromPayload(text, String.class, String.class));
    }

    @Test
    public void workflowReceivesAndReturnsMoney() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder()
                .setWorkflowClientOptions(Converters.clientOptions())
                .build());
        try {
            Worker worker = testEnv.newWorker("pricing");
            worker.registerWorkflowImplementationTypes(PricingWorkflowImpl.class);
            testEnv.start();

            PricingWorkflow workflow = testEnv.getWorkflowClient().newWorkflowStub(
                PricingWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId("pricing").setTaskQueue("pricing").build());
            assertEquals("13.57 USD", workflow.addTax(PRICE, "US").toString());

            Payload input = testEnv.getWorkflowClient().fetchHistory("pricing").getEvents().stream()
                .filter(e -> e.getEventType() == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED)
                .findFirst().orElseThrow()
                .getWorkflowExecutionStartedEventAttributes().getInput().getPayloads(0);
            assertEquals("\"12.34 USD\"", data(input));
        } finally {
            testEnv.close();
        }
    }

    // A converter with a new encoding type is added after the default Jackson converter, which
    // accepts almost any object, so it never gets the chance to convert Money.
    static class MoneyWithOwnEncoding implements PayloadConverter {
        @Override
        public String getEncodingType() {
            return "json/x-money";
        }

        @Override
        public Optional<Payload> toData(Object value) {
            throw new AssertionError("never called for Money");
        }

        @Override
        public <T> T fromData(Payload content, Class<T> valueClass, Type valueType) {
            throw new AssertionError("never called");
        }
    }

    @Test
    public void newEncodingTypeRunsAfterJackson() {
        DataConverter converter = DefaultDataConverter.newDefaultInstance()
            .withPayloadConverterOverrides(new MoneyWithOwnEncoding());
        assertEquals("json/plain", encoding(converter.toPayload(PRICE).orElseThrow()));
    }

    @Test
    public void customJacksonConverterKeepsSdkDefaults() {
        DataConverter converter = DefaultDataConverter.newDefaultInstance()
            .withPayloadConverterOverrides(Converters.yourCustomJacksonJsonPayloadConverter());
        Order order = new Order("order-1", Instant.parse("2026-01-02T03:04:05Z"));

        Payload payload = converter.toPayload(order).orElseThrow();
        assertTrue(data(payload).contains("\"order_id\":\"order-1\""), data(payload));
        assertTrue(data(payload).contains("\"placed_at\":\"2026-01-02T03:04:05Z\""), data(payload));
        assertEquals(order.placedAt, converter.fromPayload(payload, Order.class, Order.class).placedAt);

        // A plain ObjectMapper doesn't have the SDK's JavaTimeModule, so Instant fails.
        DataConverter plain = DefaultDataConverter.newDefaultInstance()
            .withPayloadConverterOverrides(new JacksonJsonPayloadConverter(new ObjectMapper()));
        assertThrows(DataConverterException.class, () -> plain.toPayload(order));
    }
}
