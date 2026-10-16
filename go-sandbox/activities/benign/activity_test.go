package benign_test

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/activities/benign"
)

type BenignErrorTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestBenignErrorTestSuite(t *testing.T) {
	suite.Run(t, new(BenignErrorTestSuite))
}

func (s *BenignErrorTestSuite) Test_ErrorIsBenign() {
	defer benign.SetCallExternalService(func() (string, error) { return "", benign.ErrNotReady })()
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(benign.MyActivity)

	_, err := env.ExecuteActivity(benign.MyActivity)
	var appErr *temporal.ApplicationError
	s.True(errors.As(err, &appErr))
	s.Equal(temporal.ApplicationErrorCategoryBenign, appErr.Category())
	s.Contains(appErr.Error(), benign.ErrNotReady.Error())
}

func (s *BenignErrorTestSuite) Test_Success() {
	defer benign.SetCallExternalService(func() (string, error) { return "ready", nil })()
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(benign.MyActivity)

	val, err := env.ExecuteActivity(benign.MyActivity)
	s.NoError(err)
	var result string
	s.NoError(val.Get(&result))
	s.Equal("ready", result)
}
