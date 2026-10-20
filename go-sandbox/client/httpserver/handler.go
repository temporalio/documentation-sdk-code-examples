package main

import (
	"encoding/json"
	"net/http"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/workflow"
)

func YourWorkflowDefinition(ctx workflow.Context) (string, error) {
	return "done", nil
}

// startWorkflowHandler starts a Workflow Execution and responds with its IDs.
func startWorkflowHandler(w http.ResponseWriter, r *http.Request, temporalClient client.Client) {
	run, err := temporalClient.ExecuteWorkflow(r.Context(), client.StartWorkflowOptions{
		TaskQueue: "your-task-queue",
	}, YourWorkflowDefinition)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]string{
		"workflowID": run.GetID(),
		"runID":      run.GetRunID(),
	})
}
