package batchiterator_test

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/workflow"

	batchiterator "my-org/quickstart/cloud-quickstarts/batch-iterator"
)

func newEnv(t *testing.T) *testsuite.TestWorkflowEnvironment {
	var ts testsuite.WorkflowTestSuite
	env := ts.NewTestWorkflowEnvironment()
	env.RegisterWorkflow(batchiterator.RecordProcessorWorkflow)
	env.RegisterActivity(batchiterator.GetRecords)
	return env
}

func TestIteratorBatchWorkflow_ProcessesPageThenContinuesAsNew(t *testing.T) {
	env := newEnv(t)
	completedChildren := 0
	env.SetOnChildWorkflowCompletedListener(func(_ *workflow.Info, _ converter.EncodedValue, err error) {
		require.NoError(t, err)
		completedChildren++
	})

	env.ExecuteWorkflow(batchiterator.IteratorBatchWorkflow, batchiterator.PageSize, 0)

	require.True(t, env.IsWorkflowCompleted())
	var canErr *workflow.ContinueAsNewError
	require.True(t, errors.As(env.GetWorkflowError(), &canErr), "expected continue-as-new, got %v", env.GetWorkflowError())
	require.Equal(t, batchiterator.PageSize, completedChildren)

	var pageSize, nextOffset int
	require.NoError(t, converter.GetDefaultDataConverter().FromPayloads(canErr.Input, &pageSize, &nextOffset))
	require.Equal(t, batchiterator.PageSize, pageSize)
	require.Equal(t, batchiterator.PageSize, nextOffset)
}

func TestIteratorBatchWorkflow_ReturnsTotalWhenNoRecordsLeft(t *testing.T) {
	env := newEnv(t)
	total := batchiterator.PageSize * batchiterator.PageCount

	env.ExecuteWorkflow(batchiterator.IteratorBatchWorkflow, batchiterator.PageSize, total)

	require.True(t, env.IsWorkflowCompleted())
	require.NoError(t, env.GetWorkflowError())
	var result int
	require.NoError(t, env.GetWorkflowResult(&result))
	require.Equal(t, total, result)
}
