package sessions_test

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/workers/sessions"
)

type SessionsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestSessionsTestSuite(t *testing.T) {
	suite.Run(t, new(SessionsTestSuite))
}

func (s *SessionsTestSuite) newEnv() *testsuite.TestWorkflowEnvironment {
	env := s.NewTestWorkflowEnvironment()
	env.SetWorkerOptions(worker.Options{EnableSessionWorker: true})
	env.RegisterActivity(&sessions.FileActivities{})
	return env
}

func (s *SessionsTestSuite) Test_SomeFileProcessingWorkflow() {
	env := s.newEnv()
	env.ExecuteWorkflow(sessions.SomeFileProcessingWorkflow, sessions.FileProcessingWFParam{CloudFileLocation: "s3://bucket/report.txt"})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
}

func (s *SessionsTestSuite) Test_ProcessFilesWorkflowContinuesWithRecreateToken() {
	env := s.newEnv()
	env.ExecuteWorkflow(sessions.ProcessFilesWorkflow, sessions.RecreateSessionParam{Files: []string{"a.txt", "b.txt"}})
	s.True(env.IsWorkflowCompleted())

	var canErr *workflow.ContinueAsNewError
	s.True(errors.As(env.GetWorkflowError(), &canErr), "expected Continue-As-New, got %v", env.GetWorkflowError())
	var next sessions.RecreateSessionParam
	s.NoError(converter.GetDefaultDataConverter().FromPayloads(canErr.Input, &next))
	s.Equal([]string{"b.txt"}, next.Files)
	s.NotEmpty(next.RecreateToken)
}
