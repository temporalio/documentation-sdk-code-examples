package io.temporal.docs.bestpractices.errorhandling;

import io.temporal.activity.Activity;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.TimeoutFailure;

// @@@SNIPSTART java-error-handling-error-normalizing-interceptor
public final class ErrorNormalizingWorkerInterceptor extends WorkerInterceptorBase {
    @Override
    public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
        return new ActivityInboundCallsInterceptorBase(next) {
            @Override
            public ActivityOutput execute(ActivityInput input) {
                try {
                    return super.execute(input);
                } catch (ApplicationFailure | TimeoutFailure | CanceledFailure f) {
                    throw f; // already a well-formed Temporal failure — pass through
                } catch (PaymentDeclinedException e) {
                    throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "PaymentDeclined", e.toDetail());
                } catch (Exception e) {
                    throw Activity.wrap(e); // uniform fallback: type = class name, retryable
                }
            }
        };
    }
}
// @@@SNIPEND
