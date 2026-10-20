// @@@SNIPSTART go-session-workflow
package sessions

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

type FileProcessingWFParam struct {
	CloudFileLocation string
}

func SomeFileProcessingWorkflow(ctx workflow.Context, param FileProcessingWFParam) error {
	activityOptions := workflow.ActivityOptions{
		StartToCloseTimeout: time.Minute,
	}
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	err := processFile(ctx, param)
	return err
}

func processFile(ctx workflow.Context, param FileProcessingWFParam) error {
	sessionOptions := &workflow.SessionOptions{
		CreationTimeout:  time.Minute,
		ExecutionTimeout: time.Minute,
	}

	sessionCtx, err := workflow.CreateSession(ctx, sessionOptions)
	if err != nil {
		return err
	}
	defer workflow.CompleteSession(sessionCtx)

	var a *FileActivities
	var downloadResult FileActivityResult

	err = workflow.ExecuteActivity(sessionCtx, a.DownloadFile, param).Get(sessionCtx, &downloadResult)
	if err != nil {
		return err
	}

	processParam := FileActivityParam(downloadResult)
	var processResult FileActivityResult
	err = workflow.ExecuteActivity(sessionCtx, a.ProcessFile, processParam).Get(sessionCtx, &processResult)
	if err != nil {
		return err
	}

	uploadParam := FileActivityParam(processResult)
	err = workflow.ExecuteActivity(sessionCtx, a.UploadFile, uploadParam).Get(sessionCtx, nil)
	return err
}
// @@@SNIPEND
