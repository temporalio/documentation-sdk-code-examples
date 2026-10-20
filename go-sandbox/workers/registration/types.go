package main

import (
	"context"

	"go.temporal.io/sdk/workflow"
)

func WorkflowA(ctx workflow.Context) error { return nil }
func WorkflowB(ctx workflow.Context) error { return nil }

// MyActivities groups related Activities. Registering the struct registers every exported method.
type MyActivities struct{}

func (a *MyActivities) ActivityOne(ctx context.Context) error { return nil }
func (a *MyActivities) ActivityTwo(ctx context.Context) error { return nil }
