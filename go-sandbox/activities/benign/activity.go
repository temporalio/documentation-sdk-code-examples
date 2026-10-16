package benign

// @@@SNIPSTART go-benign-activity-error
import (
	"context"

	"go.temporal.io/sdk/temporal"
)

func MyActivity(ctx context.Context) (string, error) {
	result, err := callExternalService()
	if err != nil {
		// Mark this error as benign since it's expected
		return "", temporal.NewApplicationErrorWithOptions(
			err.Error(),
			"",
			temporal.ApplicationErrorOptions{
				Category: temporal.ApplicationErrorCategoryBenign,
			},
		)
	}
	return result, nil
}
// @@@SNIPEND
