package io.temporal.docs.workers.interceptors;

import io.temporal.common.interceptors.WorkflowClientCallsInterceptor;
import io.temporal.common.interceptors.WorkflowClientCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowClientInterceptorBase;

// @@@SNIPSTART java-interceptors-client-interceptor
public class TracingClientInterceptor extends WorkflowClientInterceptorBase {
    @Override
    public WorkflowClientCallsInterceptor workflowClientCallsInterceptor(
        WorkflowClientCallsInterceptor next) {
        return new WorkflowClientCallsInterceptorBase(next) {
            @Override
            public WorkflowStartOutput start(WorkflowStartInput input) {
                System.out.println("starting workflow " + input.getWorkflowId());
                return super.start(input);
            }
        };
    }
}
// @@@SNIPEND
