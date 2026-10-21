package ctxpropagation

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
	commonpb "go.temporal.io/api/common/v1"
)

// header stands in for the Temporal headers the SDK passes to a propagator.
type header map[string]*commonpb.Payload

func (h header) Set(key string, value *commonpb.Payload) { h[key] = value }
func (h header) Get(key string) (*commonpb.Payload, bool) {
	v, ok := h[key]
	return v, ok
}
func (h header) ForEachKey(handler func(string, *commonpb.Payload) error) error {
	for k, v := range h {
		if err := handler(k, v); err != nil {
			return err
		}
	}
	return nil
}

func TestPropagatorRoundTrip(t *testing.T) {
	p := NewContextPropagator()
	h := header{}
	ctx := context.WithValue(context.Background(), PropagateKey, &Values{Key: "tenant", Value: "acme"})
	require.NoError(t, p.Inject(ctx, h))
	require.Contains(t, h, HeaderKey)

	out, err := p.Extract(context.Background(), h)
	require.NoError(t, err)
	require.Equal(t, Values{Key: "tenant", Value: "acme"}, out.Value(PropagateKey))
}

func TestExtractWithoutHeader(t *testing.T) {
	out, err := NewContextPropagator().Extract(context.Background(), header{})
	require.NoError(t, err)
	require.Nil(t, out.Value(PropagateKey))
}
