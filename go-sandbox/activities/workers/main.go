// @@@SNIPSTART go-activities-worker
package main

import (
	"log"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/activities/basics"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	yourWorker := worker.New(temporalClient, "your-custom-task-queue-name", worker.Options{})
	yourWorker.RegisterWorkflow(basics.YourWorkflowDefinition)
	registerWFOptions := workflow.RegisterOptions{
		Name: "JustAnotherWorkflow",
	}
	yourWorker.RegisterWorkflowWithOptions(basics.YourSimpleWorkflowDefinition, registerWFOptions)

	message := "This could be a connection string or endpoint details"
	number := 100
	activities := &basics.YourActivityObject{
		Message: &message,
		Number:  &number,
	}
	yourWorker.RegisterActivity(activities)

	registerAOptions := activity.RegisterOptions{
		Name: "JustAnotherActivity",
	}
	yourWorker.RegisterActivityWithOptions(basics.YourSimpleActivityDefinition, registerAOptions)

	err = yourWorker.Run(worker.InterruptCh())
	if err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
// @@@SNIPEND
