package childworkflows

// @@@SNIPSTART go-child-workflow-async
import (
	"go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/workflow"
)

func YourAsyncWorkflowDefinition(ctx workflow.Context, params ParentParams) (ParentResp, error) {
	childWorkflowOptions := workflow.ChildWorkflowOptions{
		ParentClosePolicy: enums.PARENT_CLOSE_POLICY_ABANDON,
	}
	ctx = workflow.WithChildOptions(ctx, childWorkflowOptions)

	childWorkflowFuture := workflow.ExecuteChildWorkflow(ctx, YourOtherWorkflowDefinition, ChildParams{Name: params.Name})
	// Wait for the Child Workflow Execution to spawn
	var childWE workflow.Execution
	if err := childWorkflowFuture.GetChildWorkflowExecution().Get(ctx, &childWE); err != nil {
		return ParentResp{}, err
	}
	// ...
	return ParentResp{ChildWorkflowID: childWE.ID}, nil
}
// @@@SNIPEND
