package cloud

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestDynamicAPIKeyCredentials(t *testing.T) {
	require.NotNil(t, DynamicAPIKeyCredentials("first", "second"))
}

func TestAPIKeyProviderHeaders(t *testing.T) {
	p := &APIKeyProvider{APIKey: "key", Namespace: "ns.acct"}
	UpdateHeadersProviderKey(p, "rotated")
	headers, err := p.GetHeaders(context.Background())
	require.NoError(t, err)
	require.Equal(t, "Bearer rotated", headers["Authorization"])
	require.Equal(t, "ns.acct", headers["temporal-namespace"])
}
