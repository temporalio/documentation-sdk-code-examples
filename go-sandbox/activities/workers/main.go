// @@@SNIPSTART go-activities-worker
package workers

import (
	"log"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"
)

func main() {
    temporalClient, err := client.Dial(client.Options{})
    if err != nil {
        log.Fatalln("Unable to create client", err)
    }
    defer temporalClient.Close()
    
    yourWorker := worker.New(temporalClient, "your-custom-task-queue-name", worker.Options{})
    yourWorker.RegisterWorkflow(yourapp.YourWorkflowDefinition)
    registerWFOptions := workflow.RegisterOptions{
        Name: "JustAnotherWorkflow",
    }
    yourWorker.RegisterWorkflowWithOptions(yourapp.YourSimpleWorkflowDefinition, registerWFOptions)
    
    message := "This could be a connection string or endpoint details"
    number := 100
    activities := &yourapp.YourActivityObject{
        Message: &message,
        Number:  &number,
    }
    
    registerAOptions := activity.RegisterOptions{
        Name: "JustAnotherActivity",
    }
    yourWorker.RegisterActivityWithOptions(yourapp.YourSimpleActivityDefinition, registerAOptions)
    
    err = yourWorker.Run(worker.InterruptCh())
    if err != nil {
        log.Fatalln("Unable to start Worker", err)
    }
}
// @@@SNIPEND