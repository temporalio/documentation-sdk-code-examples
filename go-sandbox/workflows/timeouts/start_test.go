package timeouts_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/mocks"

	"my-org/quickstart/workflows/timeouts"
)

func TestStartWithTimeout(t *testing.T) {
	c := &mocks.Client{}
	run := &mocks.WorkflowRun{}
	c.On("ExecuteWorkflow", mock.Anything, mock.MatchedBy(func(o client.StartWorkflowOptions) bool {
		return o.TaskQueue == timeouts.TaskQueueName && o.WorkflowExecutionTimeout == 24*365*10*time.Hour
	}), mock.Anything).Return(run, nil)

	got, err := timeouts.StartWithTimeout(c)
	require.NoError(t, err)
	require.Equal(t, run, got)
	c.AssertExpectations(t)
}

func TestStartWithRetryPolicy(t *testing.T) {
	c := &mocks.Client{}
	run := &mocks.WorkflowRun{}
	c.On("ExecuteWorkflow", mock.Anything, mock.MatchedBy(func(o client.StartWorkflowOptions) bool {
		return o.RetryPolicy != nil && o.RetryPolicy.InitialInterval == time.Second &&
			o.RetryPolicy.BackoffCoefficient == 2.0 && o.RetryPolicy.MaximumInterval == 100*time.Second
	}), mock.Anything).Return(run, nil)

	got, err := timeouts.StartWithRetryPolicy(c)
	require.NoError(t, err)
	require.Equal(t, run, got)
	c.AssertExpectations(t)
}
