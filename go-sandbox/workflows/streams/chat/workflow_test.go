package chat_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/streams/chat"
)

type ChatTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestChatTestSuite(t *testing.T) {
	suite.Run(t, new(ChatTestSuite))
}

func (s *ChatTestSuite) run(ack bool) time.Duration {
	env := s.NewTestWorkflowEnvironment()
	env.OnActivity(chat.StreamCompletion, mock.Anything, "hello").Return("streamed reply", nil)
	if ack {
		env.RegisterDelayedCallback(func() {
			env.SignalWorkflow("subscriber-acknowledged-terminator", nil)
		}, time.Second)
	}
	start := env.Now()
	env.ExecuteWorkflow(chat.ChatWorkflow, chat.ChatInput{Prompt: "hello"})
	s.NoError(env.GetWorkflowError())

	var result string
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal("streamed reply", result)
	return env.Now().Sub(start)
}

func (s *ChatTestSuite) Test_ReturnsAfterAck() {
	s.Less(s.run(true), 30*time.Second)
}

func (s *ChatTestSuite) Test_FallsBackToTimeout() {
	s.GreaterOrEqual(s.run(false), 30*time.Second)
}
