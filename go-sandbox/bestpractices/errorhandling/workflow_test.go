package errorhandling_test

import (
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	enumspb "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/bestpractices/errorhandling"
)

func TestHandleErrorsWorkflow(t *testing.T) {
	cases := map[string]string{
		"":               "no error",
		"CustomErrTypeA": "CustomErrTypeA: string details",
		"CustomErrTypeB": "CustomErrTypeB: string details",
		"panic":          "panic",
	}
	for failure, want := range cases {
		t.Run(failure, func(t *testing.T) {
			var suite testsuite.WorkflowTestSuite
			env := suite.NewTestWorkflowEnvironment()
			env.RegisterActivity(errorhandling.YourActivity)
			env.ExecuteWorkflow(errorhandling.HandleErrorsWorkflow, failure)
			require.NoError(t, env.GetWorkflowError())
			var outcome string
			require.NoError(t, env.GetWorkflowResult(&outcome))
			require.Equal(t, want, outcome)
		})
	}
}

func TestHandleErrorsWorkflowTimeout(t *testing.T) {
	var suite testsuite.WorkflowTestSuite
	env := suite.NewTestWorkflowEnvironment()
	// The test environment doesn't enforce Activity timeouts, so return the TimeoutError the server would.
	env.OnActivity(errorhandling.YourActivity, mock.Anything, "timeout").
		Return(temporal.NewTimeoutError(enumspb.TIMEOUT_TYPE_START_TO_CLOSE, nil))
	env.ExecuteWorkflow(errorhandling.HandleErrorsWorkflow, "timeout")
	require.NoError(t, env.GetWorkflowError())
	var outcome string
	require.NoError(t, env.GetWorkflowResult(&outcome))
	require.Equal(t, "StartToClose timeout", outcome)
}
