package childworkflows

type ParentParams struct {
	Name string
}

type ParentResp struct {
	Greeting        string
	ChildWorkflowID string
}

type ChildParams struct {
	Name string
}

type ChildResp struct {
	Greeting string
}
