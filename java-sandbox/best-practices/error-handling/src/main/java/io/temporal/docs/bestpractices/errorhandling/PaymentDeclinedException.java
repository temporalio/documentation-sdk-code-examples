package io.temporal.docs.bestpractices.errorhandling;

// A domain exception. The ErrorNormalizingWorkerInterceptor converts it to a non-retryable
// ApplicationFailure. It's unchecked so the interceptor can catch it around super.execute().
public class PaymentDeclinedException extends RuntimeException {
    private final String orderId;

    public PaymentDeclinedException(String message, String orderId) {
        super(message);
        this.orderId = orderId;
    }

    // Details to attach to the ApplicationFailure.
    public String toDetail() {
        return orderId;
    }
}
