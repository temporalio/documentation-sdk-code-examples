// @@@SNIPSTART go-client-start-workflow
package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/workflow"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()
	// ...
	workflowOptions := client.StartWorkflowOptions{
		TaskQueue: "your-task-queue",
		// ...
	}
	param := YourWorkflowParam{Name: "Temporal"}
	workflowRun, err := temporalClient.ExecuteWorkflow(context.Background(), workflowOptions, YourWorkflowDefinition, param)
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}
	log.Println("Started workflow", "WorkflowID", workflowRun.GetID(), "RunID", workflowRun.GetRunID())
	// ...
}

func YourWorkflowDefinition(ctx workflow.Context, param YourWorkflowParam) (YourWorkflowResponse, error) {
	// ...
	return YourWorkflowResponse{Greeting: "Hello " + param.Name}, nil
}
// @@@SNIPEND

type YourWorkflowParam struct {
	Name string
}

type YourWorkflowResponse struct {
	Greeting string
}
