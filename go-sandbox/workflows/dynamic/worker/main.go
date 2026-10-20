package main

import (
	"log"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"

	dynamicactivity "my-org/quickstart/activities/dynamic"
	"my-org/quickstart/workflows/dynamic"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, dynamic.TaskQueue, worker.Options{})
	// @@@SNIPSTART go-dynamic-workflow-register
	w.RegisterDynamicWorkflow(dynamic.DynamicWorkflow, workflow.DynamicRegisterOptions{})
	// @@@SNIPEND
	// The "dynamic-activity" Workflow Type calls an unregistered Activity Type,
	// which the Dynamic Activity handles.
	w.RegisterDynamicActivity(dynamicactivity.DynamicActivity, activity.DynamicRegisterOptions{})

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
