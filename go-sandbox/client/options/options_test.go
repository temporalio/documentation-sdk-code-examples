package options_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	enums "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/mocks"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/client/options"
)

func TestEachOptionReachesExecuteWorkflow(t *testing.T) {
	param := options.YourWorkflowParam{Name: "Temporal"}
	cases := []struct {
		name  string
		start func(client.Client, options.YourWorkflowParam) (client.WorkflowRun, error)
		check func(client.StartWorkflowOptions) bool
	}{
		{"TaskQueue", options.StartWithTaskQueue, func(o client.StartWorkflowOptions) bool { return o.TaskQueue == "your-task-queue" }},
		{"ID", options.StartWithID, func(o client.StartWorkflowOptions) bool { return o.ID == "Your-Custom-Workflow-Id" }},
		{"RunTimeout", options.StartWithRunTimeout, func(o client.StartWorkflowOptions) bool { return o.WorkflowRunTimeout == 24*365*10*time.Hour }},
		{"TaskTimeout", options.StartWithTaskTimeout, func(o client.StartWorkflowOptions) bool { return o.WorkflowTaskTimeout == 10*time.Second }},
		{"IDReusePolicy", options.StartWithIDReusePolicy, func(o client.StartWorkflowOptions) bool {
			return o.WorkflowIDReusePolicy == enums.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE
		}},
		{"ErrorWhenAlreadyStarted", options.StartWithErrorWhenAlreadyStarted, func(o client.StartWorkflowOptions) bool { return !o.WorkflowExecutionErrorWhenAlreadyStarted }},
		{"Memo", options.StartWithMemo, func(o client.StartWorkflowOptions) bool { return o.Memo["description"] == "Test search attributes workflow" }},
		{"SearchAttributes", options.StartWithSearchAttributes, func(o client.StartWorkflowOptions) bool { return o.SearchAttributes["MiscData"] == "yellow" }},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			mc := &mocks.Client{}
			mc.On("ExecuteWorkflow", mock.Anything, mock.MatchedBy(func(o client.StartWorkflowOptions) bool {
				return o.TaskQueue != "" && c.check(o)
			}), mock.Anything, param).Return(&mocks.WorkflowRun{}, nil)

			_, err := c.start(mc, param)
			require.NoError(t, err)
			mc.AssertExpectations(t)
		})
	}
}

func TestStartByName(t *testing.T) {
	mc := &mocks.Client{}
	mc.On("ExecuteWorkflow", mock.Anything, mock.Anything, "YourWorkflowDefinition", mock.Anything).Return(&mocks.WorkflowRun{}, nil)
	_, err := options.StartByName(mc, client.StartWorkflowOptions{TaskQueue: "q"}, options.YourWorkflowParam{})
	require.NoError(t, err)
	mc.AssertExpectations(t)
}

func TestGetResultByID(t *testing.T) {
	run := &mocks.WorkflowRun{}
	run.On("Get", mock.Anything, mock.Anything).Run(func(args mock.Arguments) {
		*(args.Get(1).(*options.YourWorkflowResponse)) = options.YourWorkflowResponse{Greeting: "Hello Temporal"}
	}).Return(nil)
	mc := &mocks.Client{}
	mc.On("GetWorkflow", mock.Anything, "Your-Custom-Workflow-Id", "").Return(run)

	result, err := options.GetResultByID(mc)
	require.NoError(t, err)
	require.Equal(t, "Hello Temporal", result.Greeting)
}

func TestCronLastCompletionResult(t *testing.T) {
	var suite testsuite.WorkflowTestSuite
	for _, c := range []struct {
		last *options.CronResult
		want int
	}{{nil, 1}, {&options.CronResult{Count: 4}, 5}} {
		env := suite.NewTestWorkflowEnvironment()
		if c.last != nil {
			env.SetLastCompletionResult(*c.last)
		}
		env.ExecuteWorkflow(options.YourCronWorkflowDefinition)
		require.NoError(t, env.GetWorkflowError())
		var result options.CronResult
		require.NoError(t, env.GetWorkflowResult(&result))
		require.Equal(t, c.want, result.Count)
	}
}
