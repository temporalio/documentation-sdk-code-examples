package sessions

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

type RecreateSessionParam struct {
	Files         []string
	RecreateToken []byte
}

// ProcessFilesWorkflow processes one file per run and continues as new with a recreate token,
// so every run uses a Session on the same Worker.
func ProcessFilesWorkflow(ctx workflow.Context, param RecreateSessionParam) error {
	if len(param.Files) == 0 {
		return nil
	}
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{StartToCloseTimeout: time.Minute})
	sessionOptions := &workflow.SessionOptions{
		CreationTimeout:  time.Minute,
		ExecutionTimeout: time.Minute,
	}

	var sessionCtx workflow.Context
	var err error
	if param.RecreateToken == nil {
		sessionCtx, err = workflow.CreateSession(ctx, sessionOptions)
	} else {
		sessionCtx, err = workflow.RecreateSession(ctx, param.RecreateToken, sessionOptions)
	}
	if err != nil {
		return err
	}

	var a *FileActivities
	var result FileActivityResult
	err = workflow.ExecuteActivity(sessionCtx, a.DownloadFile, FileProcessingWFParam{CloudFileLocation: param.Files[0]}).Get(sessionCtx, &result)
	if err != nil {
		workflow.CompleteSession(sessionCtx)
		return err
	}

	// @@@SNIPSTART go-session-recreate-token
	token := workflow.GetSessionInfo(sessionCtx).GetRecreateToken()
	// @@@SNIPEND
	workflow.CompleteSession(sessionCtx)

	return workflow.NewContinueAsNewError(ctx, ProcessFilesWorkflow, RecreateSessionParam{
		Files:         param.Files[1:],
		RecreateToken: token,
	})
}
