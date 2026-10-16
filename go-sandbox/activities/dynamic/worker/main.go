package main

import (
	"log"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/activities/dynamic"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, dynamic.TaskQueue, worker.Options{})
	w.RegisterWorkflow(dynamic.CallUnregisteredActivityWorkflow)
	// @@@SNIPSTART go-dynamic-activity-register
	w.RegisterDynamicActivity(dynamic.DynamicActivity, activity.DynamicRegisterOptions{})
	// @@@SNIPEND

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
