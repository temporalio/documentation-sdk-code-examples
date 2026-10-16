package standalone_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/activities/standalone"
)

type StandaloneActivityTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestStandaloneActivityTestSuite(t *testing.T) {
	suite.Run(t, new(StandaloneActivityTestSuite))
}

func (s *StandaloneActivityTestSuite) Test_Activity() {
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(standalone.Activity)

	val, err := env.ExecuteActivity(standalone.Activity, "Temporal")
	s.NoError(err)
	var result string
	s.NoError(val.Get(&result))
	s.Equal("Hello Temporal!", result)
}
