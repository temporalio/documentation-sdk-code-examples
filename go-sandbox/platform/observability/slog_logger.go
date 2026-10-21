package observability

// @@@SNIPSTART go-observability-slog-logger
import (
	"log/slog"
	"os"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/log"
)

func DialWithSlog() (client.Client, error) {
	// ...
	slogHandler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo})
	logger := log.NewStructuredLogger(slog.New(slogHandler))
	clientOptions := client.Options{
		Logger: logger,
	}
	temporalClient, err := client.Dial(clientOptions)
	// ...
	return temporalClient, err
}
// @@@SNIPEND
