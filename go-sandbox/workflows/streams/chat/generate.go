package chat

// generateDeltas stands in for a streaming model call. It yields the response one token at a time.
func generateDeltas(prompt string) <-chan string {
	ch := make(chan string)
	go func() {
		defer close(ch)
		for _, token := range []string{"You ", "asked: ", prompt} {
			ch <- token
		}
	}()
	return ch
}
