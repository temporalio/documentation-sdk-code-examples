package benign

import "errors"

var ErrNotReady = errors.New("external service is not ready yet")

// callExternalService stands in for a call to a service that is sometimes not
// ready yet. Tests replace it to control the outcome.
var callExternalService = func() (string, error) {
	return "", ErrNotReady
}
