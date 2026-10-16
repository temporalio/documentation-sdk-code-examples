package childworkflows

// @@@SNIPSTART go-child-workflow-parent-close-policy
import (
	"go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/workflow"
)

func YourParentClosePolicyWorkflowDefinition(ctx workflow.Context, params ParentParams) (ParentResp, error) {
	// ...
	childWorkflowOptions := workflow.ChildWorkflowOptions{
		// ...
		ParentClosePolicy: enums.PARENT_CLOSE_POLICY_ABANDON,
	}
	ctx = workflow.WithChildOptions(ctx, childWorkflowOptions)
	childWorkflowFuture := workflow.ExecuteChildWorkflow(ctx, YourOtherWorkflowDefinition, ChildParams{Name: params.Name})
	// ...
	var result ChildResp
	if err := childWorkflowFuture.Get(ctx, &result); err != nil {
		return ParentResp{}, err
	}
	return ParentResp{Greeting: result.Greeting}, nil
}
// @@@SNIPEND
