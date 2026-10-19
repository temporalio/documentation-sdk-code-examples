package schedules_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/mocks"

	"my-org/quickstart/workflows/schedules"
)

func TestStartWithDelay(t *testing.T) {
	c := &mocks.Client{}
	c.On("ExecuteWorkflow", mock.Anything, mock.MatchedBy(func(o client.StartWorkflowOptions) bool {
		return o.StartDelay == 12*time.Hour
	}), mock.Anything).Return(&mocks.WorkflowRun{}, nil)

	_, err := schedules.StartWithDelay(c)
	require.NoError(t, err)
	c.AssertExpectations(t)
}

func TestStartCronWorkflow(t *testing.T) {
	c := &mocks.Client{}
	c.On("ExecuteWorkflow", mock.Anything, mock.MatchedBy(func(o client.StartWorkflowOptions) bool {
		return o.CronSchedule == "15 8 * * *"
	}), mock.Anything).Return(&mocks.WorkflowRun{}, nil)

	_, err := schedules.StartCronWorkflow(c)
	require.NoError(t, err)
	c.AssertExpectations(t)
}
