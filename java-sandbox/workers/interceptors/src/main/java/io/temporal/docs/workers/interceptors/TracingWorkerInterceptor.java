package io.temporal.docs.workers.interceptors;

import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.workflow.Workflow;

// @@@SNIPSTART java-interceptors-worker-interceptor
public class TracingWorkerInterceptor extends WorkerInterceptorBase {
    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
        return new WorkflowInboundCallsInterceptorBase(next) {
            @Override
            public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
                next.init(
                    new WorkflowOutboundCallsInterceptorBase(outboundCalls) {
                        @Override
                        public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
                            Workflow.getLogger(TracingWorkerInterceptor.class)
                                .info("starting activity " + input.getActivityName());
                            return super.executeActivity(input);
                        }
                    });
            }
        };
    }
}
// @@@SNIPEND
