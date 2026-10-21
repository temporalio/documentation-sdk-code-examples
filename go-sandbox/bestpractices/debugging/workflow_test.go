// @@@SNIPSTART go-debugging-unit-test-suite
package debugging

import (
	"context"
	"errors"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"

	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/testsuite"
)

type UnitTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite

	env *testsuite.TestWorkflowEnvironment
}

func (s *UnitTestSuite) SetupTest() {
	s.env = s.NewTestWorkflowEnvironment()
	// Register the real Activity for tests that don't mock it.
	s.env.RegisterActivity(SimpleActivity)
}

func (s *UnitTestSuite) AfterTest(suiteName, testName string) {
	s.env.AssertExpectations(s.T())
}

func (s *UnitTestSuite) Test_SimpleWorkflow_Success() {
	s.env.ExecuteWorkflow(SimpleWorkflow, "test_success")

	s.True(s.env.IsWorkflowCompleted())
	s.NoError(s.env.GetWorkflowError())
}

func (s *UnitTestSuite) Test_SimpleWorkflow_ActivityParamCorrect() {
	s.env.OnActivity(SimpleActivity, mock.Anything, mock.Anything).Return(
		func(ctx context.Context, value string) (string, error) {
			s.Equal("test_success", value)
			return value, nil
		})
	s.env.ExecuteWorkflow(SimpleWorkflow, "test_success")

	s.True(s.env.IsWorkflowCompleted())
	s.NoError(s.env.GetWorkflowError())
}

func (s *UnitTestSuite) Test_SimpleWorkflow_ActivityFails() {
	s.env.OnActivity(SimpleActivity, mock.Anything, mock.Anything).Return(
		"", errors.New("SimpleActivityFailure"))
	s.env.ExecuteWorkflow(SimpleWorkflow, "test_failure")

	s.True(s.env.IsWorkflowCompleted())

	err := s.env.GetWorkflowError()
	s.Error(err)
	var applicationErr *temporal.ApplicationError
	s.True(errors.As(err, &applicationErr))
	s.Equal("SimpleActivityFailure", applicationErr.Error())
}

func TestUnitTestSuite(t *testing.T) {
	suite.Run(t, new(UnitTestSuite))
}
// @@@SNIPEND
