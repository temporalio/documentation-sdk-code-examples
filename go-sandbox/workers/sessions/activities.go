package sessions

import (
	"context"
	"os"
	"path/filepath"
	"strings"

	"go.temporal.io/sdk/activity"
)

type FileActivityParam struct {
	FileName string
	HostName string
}

type FileActivityResult struct {
	FileName string
	HostName string
}

// FileActivities work on local files, so every Activity in a Session must run on the same host.
type FileActivities struct{}

func (a *FileActivities) DownloadFile(ctx context.Context, param FileProcessingWFParam) (FileActivityResult, error) {
	host, _ := os.Hostname()
	fileName := filepath.Join(os.TempDir(), filepath.Base(param.CloudFileLocation))
	// Stand-in for downloading the file from cloud storage.
	if err := os.WriteFile(fileName, []byte("file contents"), 0o600); err != nil {
		return FileActivityResult{}, err
	}
	activity.GetLogger(ctx).Info("Downloaded file", "FileName", fileName)
	return FileActivityResult{FileName: fileName, HostName: host}, nil
}

func (a *FileActivities) ProcessFile(ctx context.Context, param FileActivityParam) (FileActivityResult, error) {
	data, err := os.ReadFile(param.FileName)
	if err != nil {
		return FileActivityResult{}, err
	}
	processed := param.FileName + ".processed"
	if err := os.WriteFile(processed, []byte(strings.ToUpper(string(data))), 0o600); err != nil {
		return FileActivityResult{}, err
	}
	return FileActivityResult{FileName: processed, HostName: param.HostName}, nil
}

func (a *FileActivities) UploadFile(ctx context.Context, param FileActivityParam) error {
	// Stand-in for uploading the file to cloud storage.
	if _, err := os.Stat(param.FileName); err != nil {
		return err
	}
	activity.GetLogger(ctx).Info("Uploaded file", "FileName", param.FileName)
	return nil
}
