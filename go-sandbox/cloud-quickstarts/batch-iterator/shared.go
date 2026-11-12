// Package batchiterator is a minimal sample of the iterator batch-processing
// pattern: a parent Workflow processes one page of records with child Workflows,
// then continues as new with the next offset.
package batchiterator

import "os"

// PageSize is the number of records loaded and processed per Workflow run.
const PageSize = 5

// TaskQueue is the Task Queue the Worker polls and the client targets. Keep the
// Worker and client pointed at the same value.
var TaskQueue = func() string {
	if tq := os.Getenv("TEMPORAL_TASK_QUEUE"); tq != "" {
		return tq
	}
	return "quickstart-batch-iterator"
}()

// SingleRecord is one record in the dataset being processed.
type SingleRecord struct {
	ID int
}
