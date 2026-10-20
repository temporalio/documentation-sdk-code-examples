package sideeffects_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/sideeffects"
)

type SideEffectsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestSideEffectsTestSuite(t *testing.T) {
	suite.Run(t, new(SideEffectsTestSuite))
}

func (s *SideEffectsTestSuite) Test_SideEffect() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(sideeffects.SideEffectWorkflow)
	s.NoError(env.GetWorkflowError())

	var random int
	s.NoError(env.GetWorkflowResult(&random))
	s.GreaterOrEqual(random, 0)
	s.Less(random, 100)
}

func (s *SideEffectsTestSuite) Test_MutableSideEffect() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(sideeffects.MutableSideEffectWorkflow)
	s.NoError(env.GetWorkflowError())

	var number int
	s.NoError(env.GetWorkflowResult(&number))
	s.Equal(42, number)
}
