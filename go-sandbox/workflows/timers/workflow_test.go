package timers_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/timers"
)

type TimersTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestTimersTestSuite(t *testing.T) {
	suite.Run(t, new(TimersTestSuite))
}

func (s *TimersTestSuite) Test_TimerFires() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(timers.TimerWorkflow, time.Hour)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var fired bool
	s.NoError(env.GetWorkflowResult(&fired))
	s.True(fired)
}

func (s *TimersTestSuite) Test_TimerCanceledBySignal() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterDelayedCallback(func() {
		env.SignalWorkflow(timers.CancelTimerSignal, nil)
	}, time.Minute)
	env.ExecuteWorkflow(timers.TimerWorkflow, time.Hour)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var fired bool
	s.NoError(env.GetWorkflowResult(&fired))
	s.False(fired)
}

func (s *TimersTestSuite) Test_Sleep() {
	env := s.NewTestWorkflowEnvironment()
	start := env.Now()
	env.ExecuteWorkflow(timers.SleepWorkflow)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
	s.GreaterOrEqual(env.Now().Sub(start), 10*time.Second)
}
