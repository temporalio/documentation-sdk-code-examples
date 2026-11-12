package batchiterator

import (
	"fmt"
	"math/rand"
	"time"

	"go.temporal.io/sdk/workflow"
)

// IteratorBatchWorkflow implements the iterator pattern.
//
// A single run processes one page of records in parallel, starting one
// RecordProcessorWorkflow child per record. After every child completes, the
// Workflow continues as new with the next offset, so each run's Event History
// stays small no matter how large the dataset is.
//
// pageSize is the number of records to process in a single run. offset is the
// offset of the first record to process; 0 starts the batch. It returns the
// total number of processed records.
func IteratorBatchWorkflow(ctx workflow.Context, pageSize, offset int) (int, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 5 * time.Second,
	})

	var records []SingleRecord
	if err := workflow.ExecuteActivity(ctx, GetRecords, pageSize, offset).Get(ctx, &records); err != nil {
		return 0, err
	}

	// No more records in the dataset: the batch is done.
	if len(records) == 0 {
		return offset, nil
	}

	futures := make([]workflow.ChildWorkflowFuture, 0, len(records))
	for _, record := range records {
		// Human-friendly child ID, e.g. "batch-iterator-123/7".
		childCtx := workflow.WithChildOptions(ctx, workflow.ChildWorkflowOptions{
			WorkflowID: fmt.Sprintf("%s/%d", workflow.GetInfo(ctx).WorkflowExecution.ID, record.ID),
		})
		futures = append(futures, workflow.ExecuteChildWorkflow(childCtx, RecordProcessorWorkflow, record))
	}
	// Wait for every child in this page.
	for _, f := range futures {
		if err := f.Get(ctx, nil); err != nil {
			return 0, err
		}
	}

	return 0, workflow.NewContinueAsNewError(ctx, IteratorBatchWorkflow, pageSize, offset+len(records))
}

// RecordProcessorWorkflow processes a single record.
func RecordProcessorWorkflow(ctx workflow.Context, record SingleRecord) error {
	// Simulate 1-3 seconds of work. Kept short so the whole batch finishes in
	// seconds. SideEffect records the random value in Event History so replay
	// stays deterministic.
	var seconds int
	if err := workflow.SideEffect(ctx, func(workflow.Context) interface{} {
		return 1 + rand.Intn(3)
	}).Get(&seconds); err != nil {
		return err
	}
	if err := workflow.Sleep(ctx, time.Duration(seconds)*time.Second); err != nil {
		return err
	}
	workflow.GetLogger(ctx).Info("Processed", "RecordID", record.ID)
	return nil
}
