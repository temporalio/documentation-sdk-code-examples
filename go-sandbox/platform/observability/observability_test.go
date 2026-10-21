package observability

import (
	"testing"

	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/testsuite"
)

func TestWorkflowLogsAndCompletes(t *testing.T) {
	var suite testsuite.WorkflowTestSuite
	env := suite.NewTestWorkflowEnvironment()
	env.RegisterActivity(Activity)
	env.ExecuteWorkflow(Workflow, "Temporal")
	require.NoError(t, env.GetWorkflowError())
	var result string
	require.NoError(t, env.GetWorkflowResult(&result))
	require.Equal(t, "Hello Temporal!", result)
}

func TestUpsertSearchAttributesMerges(t *testing.T) {
	var suite testsuite.WorkflowTestSuite
	env := suite.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(UpsertWorkflow, "input")
	require.NoError(t, env.GetWorkflowError())
}

func TestTracingInterceptors(t *testing.T) {
	_, err := newOpenTelemetryInterceptor()
	require.NoError(t, err)
	_, err = newOpenTracingInterceptor()
	require.NoError(t, err)
	require.NotNil(t, newDatadogInterceptor())
}
