package versioning

import "context"

func ActivityA(ctx context.Context, data string) (string, error) { return "A:" + data, nil }
func ActivityB(ctx context.Context, data string) (string, error) { return "B:" + data, nil }
func ActivityC(ctx context.Context, data string) (string, error) { return "C:" + data, nil }
func ActivityD(ctx context.Context, data string) (string, error) { return "D:" + data, nil }
func ActivityE(ctx context.Context, data string) (string, error) { return "E:" + data, nil }

func SendTextMessage(ctx context.Context) error { return nil }
func SendTweet(ctx context.Context) error       { return nil }
