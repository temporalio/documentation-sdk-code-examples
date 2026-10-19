package main

import (
	"context"
	"flag"
	"log"
	"time"

	enumspb "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/client"

	"my-org/quickstart/workflows/messagepassing/message"
)

func main() {
	command := flag.String("command", "greeting", "one of: greeting, signal-with-start, update-with-start")
	flag.Parse()

	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	switch *command {
	case "greeting":
		runGreeting(temporalClient)
	case "signal-with-start":
		signalWithStart(temporalClient)
	case "update-with-start":
		updateWithStart(temporalClient)
	default:
		log.Fatalln("Unknown command", *command)
	}
}

// runGreeting starts GreetingWorkflow, then sends it a Query, an Update, and a Signal.
func runGreeting(temporalClient client.Client) {
	we, err := temporalClient.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        "message-passing-greeting",
		TaskQueue: message.TaskQueue,
	}, message.GreetingWorkflow)
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}

	queryLanguages(temporalClient, we)
	setLanguage(temporalClient, we)
	approve(temporalClient, we)

	var greeting string
	if err := we.Get(context.Background(), &greeting); err != nil {
		log.Fatalln("Unable to get workflow result", err)
	}
	log.Println("Greeting:", greeting)
}

func queryLanguages(temporalClient client.Client, we client.WorkflowRun) {
	// @@@SNIPSTART go-message-send-query
	supportedLangResult, err := temporalClient.QueryWorkflow(context.Background(), we.GetID(), we.GetRunID(), message.GetLanguagesQuery, message.GetLanguagesInput{IncludeUnsupported: false})
	if err != nil {
		log.Fatalf("Unable to query workflow: %v", err)
	}
	var supportedLang []message.Language
	err = supportedLangResult.Get(&supportedLang)
	if err != nil {
		log.Fatalf("Unable to get query result: %v", err)
	}
	log.Println("Supported languages:", supportedLang)
	// @@@SNIPEND
}

func approve(temporalClient client.Client, we client.WorkflowRun) {
	// @@@SNIPSTART go-message-send-signal
	err := temporalClient.SignalWorkflow(context.Background(), we.GetID(), we.GetRunID(), message.ApproveSignal, message.ApproveInput{Name: ""})
	if err != nil {
		log.Fatalf("Unable to signal workflow: %v", err)
	}
	// @@@SNIPEND
}

func signalWithStart(temporalClient client.Client) {
	// @@@SNIPSTART go-message-signal-with-start
	signal := message.MySignal{
		Message: "Some important data",
	}
	workflowOptions := client.StartWorkflowOptions{
		TaskQueue: message.TaskQueue,
	}
	we, err := temporalClient.SignalWithStartWorkflow(context.Background(), "your-workflow-id", "your-signal-name", signal,
		workflowOptions, message.SignalAsyncWorkflow, message.YourWorkflowParam{})
	if err != nil {
		log.Fatalln("Error sending the Signal", err)
	}
	// @@@SNIPEND
	log.Println("Signaled Workflow", "WorkflowID", we.GetID(), "RunID", we.GetRunID())
}

func setLanguage(temporalClient client.Client, we client.WorkflowRun) {
	// @@@SNIPSTART go-message-send-update
	ctxWithTimeout, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	updateHandle, err := temporalClient.UpdateWorkflow(ctxWithTimeout, client.UpdateWorkflowOptions{
		WorkflowID:   we.GetID(),
		RunID:        we.GetRunID(),
		UpdateName:   message.SetLanguageUpdate,
		WaitForStage: client.WorkflowUpdateStageAccepted,
		Args:         []interface{}{message.Chinese},
	})
	if err != nil {
		log.Fatalf("Unable to update workflow: %v", err)
	}

	var previousLang message.Language
	err = updateHandle.Get(ctxWithTimeout, &previousLang)
	if err != nil {
		log.Fatalf("Unable to get update result: %v", err)
	}
	// @@@SNIPEND
	log.Println("Previous language:", previousLang)
}

func updateWithStart(temporalClient client.Client) {
	// @@@SNIPSTART go-message-update-with-start
	ctxWithTimeout, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	workflowOptions := client.StartWorkflowOptions{
		ID:                       "some-workflow-id",
		TaskQueue:                message.TaskQueue,
		WorkflowIDConflictPolicy: enumspb.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING,
	}

	updateOptions := client.UpdateWorkflowOptions{
		UpdateName:   message.SetLanguageUpdate,
		WaitForStage: client.WorkflowUpdateStageCompleted,
		Args:         []interface{}{message.Chinese},
	}

	startWorkflowOp := temporalClient.NewWithStartWorkflowOperation(workflowOptions, message.GreetingWorkflow)
	updateHandle, err := temporalClient.UpdateWithStartWorkflow(
		ctxWithTimeout,
		client.UpdateWithStartWorkflowOptions{
			StartWorkflowOperation: startWorkflowOp,
			UpdateOptions:          updateOptions,
		})
	if err != nil {
		log.Fatalf("Unable to execute update-with-start: %v", err)
	}

	var previousLang message.Language
	err = updateHandle.Get(ctxWithTimeout, &previousLang)
	if err != nil {
		log.Fatalf("Unable to obtain update result: %v", err)
	}

	workflowRun, err := startWorkflowOp.Get(ctxWithTimeout)
	if err != nil {
		log.Fatalf("Unable to obtain workflow run: %v", err)
	}
	// @@@SNIPEND
	log.Println("Previous language:", previousLang, "WorkflowID", workflowRun.GetID())
}
