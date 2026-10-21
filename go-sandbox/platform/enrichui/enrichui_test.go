package enrichui_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/platform/enrichui"
)

type EnrichUITestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestEnrichUITestSuite(t *testing.T) {
	suite.Run(t, new(EnrichUITestSuite))
}

func (s *EnrichUITestSuite) run(wf interface{}, want string) {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(enrichui.YourActivity)
	env.ExecuteWorkflow(wf, "input")
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
	var result string
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal(want, result)
}

func (s *EnrichUITestSuite) Test_CurrentDetails() {
	s.run(enrichui.YourWorkflow, "Workflow completed")
}

func (s *EnrichUITestSuite) Test_ActivitySummary() {
	s.run(enrichui.ActivitySummaryWorkflow, "processed input")
}

func (s *EnrichUITestSuite) Test_TimerSummary() {
	s.run(enrichui.TimerSummaryWorkflow, "Timer completed")
}
