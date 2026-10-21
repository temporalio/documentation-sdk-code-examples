package observability

// @@@SNIPSTART go-observability-zap-logger
import (
	"log/slog"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/log"
	"go.uber.org/zap"
	"go.uber.org/zap/exp/zapslog"
)

func DialWithZap() (client.Client, error) {
	// ...
	zapLogger, err := zap.NewProduction()
	if err != nil {
		return nil, err
	}
	handler := zapslog.NewHandler(zapLogger.Core())
	logger := log.NewStructuredLogger(slog.New(handler))
	clientOptions := client.Options{
		Logger: logger,
	}
	temporalClient, err := client.Dial(clientOptions)
	// ...
	return temporalClient, err
}
// @@@SNIPEND
