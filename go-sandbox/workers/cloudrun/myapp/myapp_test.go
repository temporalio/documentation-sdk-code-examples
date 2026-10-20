package myapp_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workers/cloudrun/myapp"
)

type MyAppTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestMyAppTestSuite(t *testing.T) {
	suite.Run(t, new(MyAppTestSuite))
}

func (s *MyAppTestSuite) Test_MyWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(myapp.MyActivity)
	env.ExecuteWorkflow(myapp.MyWorkflow, myapp.MyInput{Items: []string{"a", "b"}})
	s.NoError(env.GetWorkflowError())

	var result string
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal("done", result)
}

func (s *MyAppTestSuite) Test_MyActivityResumesFromHeartbeat() {
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(myapp.MyActivity)
	// A previous attempt finished items 0 and 1.
	env.SetHeartbeatDetails(1)
	var first int = -1
	env.SetOnActivityHeartbeatListener(func(info *activity.Info, details converter.EncodedValues) {
		if first == -1 {
			s.NoError(details.Get(&first))
		}
	})

	_, err := env.ExecuteActivity(myapp.MyActivity, myapp.MyInput{Items: []string{"a", "b", "c", "d"}})
	s.NoError(err)
	s.Equal(2, first)
}
