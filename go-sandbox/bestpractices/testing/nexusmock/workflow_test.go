package nexusmock

import (
	"context"
	"testing"
	"time"

	"github.com/nexus-rpc/sdk-go/nexus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/temporalnexus"
	"go.temporal.io/sdk/testsuite"
)

type UnitTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite

	env *testsuite.TestWorkflowEnvironment
}

func (s *UnitTestSuite) SetupTest() {
	s.env = s.NewTestWorkflowEnvironment()
}

func TestUnitTestSuite(t *testing.T) {
	suite.Run(t, new(UnitTestSuite))
}

// @@@SNIPSTART go-testing-nexus-mock-sync
func (s *UnitTestSuite) Test_SimpleWorkflow_NexusSyncOperation() {
	s.env.OnNexusOperation(
		"sample-service",
		nexus.NewOperationReference[SampleInput, SampleOutput]("sample-operation"),
		SampleInput{},
		mock.Anything, // NexusOperationOptions
	).Return(
		&nexus.HandlerStartOperationResultSync[SampleOutput]{
			Value: SampleOutput{},
		},
		nil, // error if you want to simulate an error in the ExecuteOperation call
	)
	// You can also add a delay to return the mock values by calling After().
	// Eg: s.env.OnNexusOperation(...).Return(...).After(1*time.Second)

	s.env.ExecuteWorkflow(SimpleWorkflow, "test_nexus_operation")

	s.True(s.env.IsWorkflowCompleted())
	s.NoError(s.env.GetWorkflowError())
}
// @@@SNIPEND

// @@@SNIPSTART go-testing-nexus-mock-async
func (s *UnitTestSuite) Test_SimpleWorkflow_NexusAsyncOperation() {
	s.env.OnNexusOperation(
		"sample-service",
		nexus.NewOperationReference[SampleInput, SampleOutput]("sample-operation"),
		SampleInput{},
		mock.Anything, // NexusOperationOptions
	).Return(
		&nexus.HandlerStartOperationResultAsync{
			OperationToken: "sample-operation-token",
		},
		nil, // error if you want to simulate an error in the ExecuteOperation call
	)
	err := s.env.RegisterNexusAsyncOperationCompletion(
		"sample-service",
		"sample-operation",
		"sample-operation-token", // must match the OperationToken above
		SampleOutput{},
		nil,           // error if you want to simulate an error in the operation
		2*time.Second, // delay to simulate how long the operation takes after it starts
	)
	s.NoError(err)
	s.env.ExecuteWorkflow(SimpleWorkflow, "test_nexus_operation")

	s.True(s.env.IsWorkflowCompleted())
	s.NoError(s.env.GetWorkflowError())
}
// @@@SNIPEND

// @@@SNIPSTART go-testing-nexus-override-sync
func (s *UnitTestSuite) Test_SimpleWorkflow_NexusSyncOperationOverride() {
	var SampleOperation = nexus.NewSyncOperation(
		"sample-operation",
		func(ctx context.Context, input SampleInput, options nexus.StartOperationOptions) (SampleOutput, error) {
			// Custom logic here.
			return SampleOutput{}, nil
		},
	)

	service := nexus.NewService("sample-service")
	s.NoError(service.Register(SampleOperation))
	s.env.RegisterNexusService(service)
	s.env.ExecuteWorkflow(SimpleWorkflow, "test_nexus_operation")

	s.True(s.env.IsWorkflowCompleted())
	s.NoError(s.env.GetWorkflowError())
}
// @@@SNIPEND

// @@@SNIPSTART go-testing-nexus-override-async
func (s *UnitTestSuite) Test_SimpleWorkflow_NexusAsyncOperationOverride() {
	// The handler Workflow must be a named function that is registered with the test environment.
	s.env.RegisterWorkflow(SampleHandlerWorkflow)
	SampleOperation := temporalnexus.NewWorkflowRunOperation(
		"sample-operation",
		SampleHandlerWorkflow,
		func(ctx context.Context, input SampleInput, options nexus.StartOperationOptions) (client.StartWorkflowOptions, error) {
			// Custom logic to build client.StartWorkflowOptions.
			return client.StartWorkflowOptions{ID: "sample-handler-workflow"}, nil
		},
	)

	service := nexus.NewService("sample-service")
	s.NoError(service.Register(SampleOperation))
	s.env.RegisterNexusService(service)
	s.env.ExecuteWorkflow(SimpleWorkflow, "test_nexus_operation")

	s.True(s.env.IsWorkflowCompleted())
	s.NoError(s.env.GetWorkflowError())
}
// @@@SNIPEND
