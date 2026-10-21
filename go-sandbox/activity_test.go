// @@@SNIPSTART go-testing-activity-suite
package quickstart_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart"
)

type ActivityTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestActivityTestSuite(t *testing.T) {
	suite.Run(t, new(ActivityTestSuite))
}

func (s *ActivityTestSuite) TestGreet() {
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(quickstart.Greet)

	result, err := env.ExecuteActivity(quickstart.Greet, "Temporal")

	s.NoError(err)

	var greeting string
	s.NoError(result.Get(&greeting))
	s.Equal("Hello Temporal", greeting)
}
// @@@SNIPEND
