package benign

// SetCallExternalService lets tests in package benign_test replace the external call.
func SetCallExternalService(f func() (string, error)) (restore func()) {
	original := callExternalService
	callExternalService = f
	return func() { callExternalService = original }
}
