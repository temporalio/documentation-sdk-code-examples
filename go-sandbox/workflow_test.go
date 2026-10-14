package quickstart_test

import (
	"testing"

	"my-org/quickstart"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"
)

type UnitTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestUnitTestSuite(t *testing.T) {
	suite.Run(t, new(UnitTestSuite))
}

func Test_Workflow(t *testing.T) {
	testSuite := &testsuite.WorkflowTestSuite{}

	env := testSuite.NewTestWorkflowEnvironment()
	env.RegisterActivity(quickstart.Greet)

	env.OnActivity(quickstart.Greet, mock.Anything, "Temporal").Return("Hello, Temporal", nil)

	env.ExecuteWorkflow(quickstart.SayHelloWorkflow, "Temporal")

	require.True(t, env.IsWorkflowCompleted())
	require.NoError(t, env.GetWorkflowError())

	var result string
	require.NoError(t, env.GetWorkflowResult(&result))
	require.Equal(t, "Hello, Temporal", result)
}
