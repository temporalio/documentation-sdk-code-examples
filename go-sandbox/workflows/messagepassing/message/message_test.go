package message_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/messagepassing/message"
)

type MessagePassingTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestMessagePassingTestSuite(t *testing.T) {
	suite.Run(t, new(MessagePassingTestSuite))
}

func (s *MessagePassingTestSuite) approveAfter(env *testsuite.TestWorkflowEnvironment, d time.Duration) {
	env.RegisterDelayedCallback(func() {
		env.SignalWorkflow(message.ApproveSignal, message.ApproveInput{Name: "approver"})
	}, d)
}

func (s *MessagePassingTestSuite) update(env *testsuite.TestWorkflowEnvironment, name string, onComplete func(any, error), onReject func(error), args ...any) {
	env.UpdateWorkflow(name, "", &testsuite.TestUpdateCallback{
		OnAccept: func() {},
		OnReject: func(err error) {
			if onReject == nil {
				s.Fail("unexpected rejection", err)
				return
			}
			onReject(err)
		},
		OnComplete: func(result any, err error) {
			if onComplete != nil {
				onComplete(result, err)
			}
		},
	}, args...)
}

func (s *MessagePassingTestSuite) Test_GreetingWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterDelayedCallback(func() {
		val, err := env.QueryWorkflow(message.GetLanguagesQuery, message.GetLanguagesInput{})
		s.NoError(err)
		var langs []message.Language
		s.NoError(val.Get(&langs))
		s.Equal([]message.Language{message.Chinese, message.English}, langs)

		s.update(env, message.SetLanguageUpdate, nil, func(err error) {
			s.ErrorContains(err, "unsupported language")
		}, message.French)
		s.update(env, message.SetLanguageUpdate, func(result any, err error) {
			s.NoError(err)
			s.Equal(message.English, result)
		}, nil, message.Chinese)
	}, time.Second)
	s.approveAfter(env, 2*time.Second)

	env.ExecuteWorkflow(message.GreetingWorkflow)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
	var greeting string
	s.NoError(env.GetWorkflowResult(&greeting))
	s.Equal("你好，世界", greeting)
}

func (s *MessagePassingTestSuite) Test_SignalAsyncWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterDelayedCallback(func() {
		env.SignalWorkflow("your-signal-name", message.MySignal{Message: "hi"})
	}, time.Second)
	env.ExecuteWorkflow(message.SignalAsyncWorkflow, message.YourWorkflowParam{})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
}

func (s *MessagePassingTestSuite) Test_DrainSignalsWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterDelayedCallback(func() {
		for _, id := range []int{1, 2, 3} {
			env.SignalWorkflow("ReportCompletion", id)
		}
	}, time.Second)
	env.ExecuteWorkflow(message.DrainSignalsWorkflow)
	s.NoError(env.GetWorkflowError())
	var count int
	s.NoError(env.GetWorkflowResult(&count))
	s.Equal(3, count)
}

func (s *MessagePassingTestSuite) Test_SendExternalSignalWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.OnSignalExternalWorkflow(mock.Anything, "some-workflow-id", "", "your-signal-name", message.MySignal{Message: "Some important data"}).Return(nil).Once()
	env.ExecuteWorkflow(message.SendExternalSignalWorkflow, message.YourWorkflowParam{})
	s.NoError(env.GetWorkflowError())
	env.AssertExpectations(s.T())
}

func (s *MessagePassingTestSuite) Test_BlockingGreetingWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(message.CallGreetingService)
	env.RegisterDelayedCallback(func() {
		s.update(env, message.SetLanguageUpdate, func(result any, err error) {
			s.NoError(err)
			s.Equal(message.English, result)
		}, nil, message.French)
	}, time.Second)
	s.approveAfter(env, 2*time.Second)

	env.ExecuteWorkflow(message.BlockingGreetingWorkflow)
	s.NoError(env.GetWorkflowError())
	var greeting string
	s.NoError(env.GetWorkflowResult(&greeting))
	s.Equal("Hello in french", greeting)
}

func (s *MessagePassingTestSuite) Test_AwaitInHandlerWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	// The Update arrives before setup finishes, so its handler waits on the condition.
	env.RegisterDelayedCallback(func() {
		s.update(env, "UpdateHandler", func(result any, err error) { s.NoError(err) }, nil, message.UpdateInput{Value: "x"})
	}, time.Millisecond)
	s.approveAfter(env, 2*time.Second)

	env.ExecuteWorkflow(message.AwaitInHandlerWorkflow)
	s.NoError(env.GetWorkflowError())
	var handled int
	s.NoError(env.GetWorkflowResult(&handled))
	s.Equal(1, handled)
}

func (s *MessagePassingTestSuite) Test_WaitForHandlersWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(message.WaitForHandlersWorkflow, message.YourWorkflowParam{})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
}

func (s *MessagePassingTestSuite) Test_UnfinishedPolicyWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterDelayedCallback(func() {
		s.update(env, message.UpdateHandlerName, nil, nil)
	}, time.Second)
	s.approveAfter(env, 2*time.Second)

	env.ExecuteWorkflow(message.UnfinishedPolicyWorkflow)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
}

func (s *MessagePassingTestSuite) Test_SafeHandlerWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(message.FetchData)
	env.RegisterDelayedCallback(func() {
		for i := 0; i < 3; i++ {
			s.update(env, "SafeUpdateHandler", func(result any, err error) { s.NoError(err) }, nil)
		}
	}, time.Second)
	s.approveAfter(env, 10*time.Second)

	env.ExecuteWorkflow(message.SafeHandlerWorkflow, message.YourWorkflowParam{Name: "temporal"})
	s.NoError(env.GetWorkflowError())
	var data message.Data
	s.NoError(env.GetWorkflowResult(&data))
	s.Equal(message.Data{X: 8, Y: 16}, data)
}

func (s *MessagePassingTestSuite) Test_BadHandlerWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(message.FetchData)
	env.RegisterDelayedCallback(func() {
		s.update(env, "BadUpdateHandler", func(result any, err error) { s.NoError(err) }, nil)
	}, time.Second)
	s.approveAfter(env, 10*time.Second)

	env.ExecuteWorkflow(message.BadHandlerWorkflow, message.YourWorkflowParam{Name: "temporal"})
	s.NoError(env.GetWorkflowError())
}
