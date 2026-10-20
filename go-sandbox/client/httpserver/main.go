// @@@SNIPSTART go-client-connect-code
package main

import (
	"log"
	"net/http"

	"go.temporal.io/sdk/client"
)

func main() {
	// Create a Temporal Client to communicate with the Temporal Service.
	// A Temporal Client should be created once per process.
	temporalClient, err := client.Dial(client.Options{
		HostPort: client.DefaultHostPort,
	})
	if err != nil {
		log.Fatalln("Unable to create Temporal Client", err)
	}
	defer temporalClient.Close()

	// Start an HTTP server and listen on /start
	http.HandleFunc("/start", func(w http.ResponseWriter, r *http.Request) {
		startWorkflowHandler(w, r, temporalClient)
	})
	err = http.ListenAndServe(":8091", nil)
	if err != nil {
		log.Fatalln("Unable to run http server", err)
	}
}
// @@@SNIPEND
