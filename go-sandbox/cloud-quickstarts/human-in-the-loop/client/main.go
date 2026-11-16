package main

import (
	"context"
	"errors"
	"log"
	"os"

	"go.temporal.io/api/serviceerror"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"

	humanintheloop "my-org/quickstart/cloud-quickstarts/human-in-the-loop"
)

const defaultRequest = "Delete all test data from the production database"

func main() {
	userRequest := defaultRequest
	if len(os.Args) > 1 {
		userRequest = os.Args[1]
	}

	// Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
	// selects the profile, the TOML file supplies address/namespace/credentials,
	// and TEMPORAL_* env vars can override individual fields.
	opts, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		log.Fatalln("Unable to load client options", err)
	}
	if opts.HostPort == "" {
		opts.HostPort = client.DefaultHostPort
	}
	if opts.Namespace == "" {
		opts.Namespace = client.DefaultNamespace
	}

	c, err := client.Dial(opts)
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	run, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        humanintheloop.WorkflowID,
		TaskQueue: humanintheloop.TaskQueue,
		// Without this, starting while a run with the same ID is open returns
		// that run instead of an error.
		WorkflowExecutionErrorWhenAlreadyStarted: true,
	}, humanintheloop.HumanInTheLoopWorkflow, humanintheloop.WorkflowInput{
		UserRequest:            userRequest,
		ApprovalTimeoutSeconds: 300,
	})
	var alreadyStarted *serviceerror.WorkflowExecutionAlreadyStarted
	if errors.As(err, &alreadyStarted) {
		log.Fatalf("Workflow %s is already running. Approve or reject it, wait for it to time out, or terminate it:\n"+
			"  temporal workflow terminate --workflow-id %s", humanintheloop.WorkflowID, humanintheloop.WorkflowID)
	}
	if err != nil {
		log.Fatalln("Unable to start Workflow", err)
	}

	log.Printf("Started Workflow %s against %s (namespace: %s)...", run.GetID(), opts.HostPort, opts.Namespace)
	log.Printf("Request: %s", userRequest)
	log.Println("If the action is risky, the Worker prints how to approve or reject it.")

	var result string
	if err := run.Get(context.Background(), &result); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Printf("Result: %s", result)
}
