package childworkflows

import "go.temporal.io/sdk/workflow"

// @@@SNIPSTART go-child-workflow-definition
func YourOtherWorkflowDefinition(ctx workflow.Context, params ChildParams) (ChildResp, error) {
	// ...
	return ChildResp{Greeting: "Hello " + params.Name}, nil
}
// @@@SNIPEND
