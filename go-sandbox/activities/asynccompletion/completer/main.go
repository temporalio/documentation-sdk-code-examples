package main

import (
	"encoding/base64"
	"flag"
	"log"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/activities/asynccompletion"
)

func main() {
	token := flag.String("token", "", "base64-encoded Task Token printed by the Worker")
	reject := flag.Bool("reject", false, "fail the Activity instead of completing it")
	flag.Parse()

	taskToken, err := base64.StdEncoding.DecodeString(*token)
	if err != nil {
		log.Fatalln("Unable to decode Task Token", err)
	}

	if !*reject {
		if err := asynccompletion.CompleteActivity(taskToken, "approver@example.com"); err != nil {
			log.Fatalln("Unable to complete Activity", err)
		}
		return
	}

	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	if err := asynccompletion.FailActivity(temporalClient, taskToken); err != nil {
		log.Fatalln("Unable to fail Activity", err)
	}
}
