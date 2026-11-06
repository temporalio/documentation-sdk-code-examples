package io.temporal.docs.bestpractices.externalstorage;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

@WorkflowInterface
public interface EchoWorkflow {
    @WorkflowMethod
    String echo(String value);

    @ActivityInterface
    interface EchoActivities {
        String echo(String value);
    }

    class EchoActivitiesImpl implements EchoActivities {
        @Override
        public String echo(String value) {
            return value;
        }
    }

    class Impl implements EchoWorkflow {
        private final EchoActivities activities = Workflow.newActivityStub(
            EchoActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

        @Override
        public String echo(String value) {
            return activities.echo(value);
        }
    }
}
