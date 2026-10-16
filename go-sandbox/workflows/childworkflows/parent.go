package childworkflows

import "go.temporal.io/sdk/workflow"

// @@@SNIPSTART go-child-workflow-execute
func YourWorkflowDefinition(ctx workflow.Context, params ParentParams) (ParentResp, error) {
	childWorkflowOptions := workflow.ChildWorkflowOptions{}
	ctx = workflow.WithChildOptions(ctx, childWorkflowOptions)

	var result ChildResp
	err := workflow.ExecuteChildWorkflow(ctx, YourOtherWorkflowDefinition, ChildParams{Name: params.Name}).Get(ctx, &result)
	if err != nil {
		return ParentResp{}, err
	}
	// ...
	return ParentResp{Greeting: result.Greeting}, nil
}
// @@@SNIPEND
