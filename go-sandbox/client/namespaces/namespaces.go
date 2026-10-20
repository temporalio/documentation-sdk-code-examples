// Package namespaces registers and manages Namespaces on a self-hosted Temporal Service.
// These APIs don't work with Temporal Cloud.
package namespaces

import (
	"context"
	"time"

	namespacepb "go.temporal.io/api/namespace/v1"
	"go.temporal.io/api/operatorservice/v1"
	"go.temporal.io/api/workflowservice/v1"
	"go.temporal.io/sdk/client"
	"google.golang.org/protobuf/types/known/durationpb"
)

// RegisterNamespace registers a Namespace with a 3-day Retention Period.
func RegisterNamespace(ctx context.Context, namespaceName string) error {
	// @@@SNIPSTART go-namespace-register
	namespaceClient, err := client.NewNamespaceClient(client.Options{HostPort: client.DefaultHostPort})
	if err != nil {
		return err
	}
	defer namespaceClient.Close()

	retention := 3 * 24 * time.Hour
	err = namespaceClient.Register(ctx, &workflowservice.RegisterNamespaceRequest{
		Namespace:                        namespaceName,
		WorkflowExecutionRetentionPeriod: durationpb.New(retention),
	})
	// @@@SNIPEND
	return err
}

// UpdateNamespace updates the description and owner of a Namespace.
func UpdateNamespace(ctx context.Context, namespaceClient client.NamespaceClient) error {
	// @@@SNIPSTART go-namespace-update
	err := namespaceClient.Update(ctx, &workflowservice.UpdateNamespaceRequest{
		Namespace: "your-namespace-name",
		UpdateInfo: &namespacepb.UpdateNamespaceInfo{ // updates info for the namespace "your-namespace-name"
			Description: "updated namespace description",
			OwnerEmail:  "newowner@mail.com",
			// Data:  nil,
			// State: 0,
		},
		/* other details that you can update:
		Config: &namespacepb.NamespaceConfig{ // updates the configuration of the namespace with the following options
			// WorkflowExecutionRetentionTtl: nil,
			// BadBinaries:                   nil,
			// HistoryArchivalState:          0,
			// HistoryArchivalUri:            "",
			// VisibilityArchivalState:       0,
			// VisibilityArchivalUri:         "",
		},
		ReplicationConfig: &replication.NamespaceReplicationConfig{ // updates the replication configuration for the namespace
			// ActiveClusterName: "",
			// Clusters:          nil,
			// State:             0,
		},
		SecurityToken:    "",
		DeleteBadBinary:  "",
		PromoteNamespace: false,
		*/
	})
	// @@@SNIPEND
	return err
}

// DescribeNamespace returns information and configuration for the default Namespace.
func DescribeNamespace(ctx context.Context) (*workflowservice.DescribeNamespaceResponse, error) {
	// @@@SNIPSTART go-namespace-describe
	namespaceClient, err := client.NewNamespaceClient(client.Options{})
	if err != nil {
		return nil, err
	}
	defer namespaceClient.Close()

	description, err := namespaceClient.Describe(ctx, "default")
	// @@@SNIPEND
	return description, err
}

// ListNamespaces returns the names of every Namespace on the Temporal Service.
func ListNamespaces(ctx context.Context, temporalClient client.Client) ([]string, error) {
	// @@@SNIPSTART go-namespace-list
	var names []string
	var nextPageToken []byte
	for {
		// Each call returns one page of Namespaces. Loop until NextPageToken is empty.
		resp, err := temporalClient.WorkflowService().ListNamespaces(ctx, &workflowservice.ListNamespacesRequest{
			PageSize:      100,
			NextPageToken: nextPageToken,
		})
		if err != nil {
			return nil, err
		}
		for _, ns := range resp.GetNamespaces() {
			names = append(names, ns.GetNamespaceInfo().GetName())
		}
		nextPageToken = resp.GetNextPageToken()
		if len(nextPageToken) == 0 {
			break
		}
	}
	// @@@SNIPEND
	return names, nil
}

// DeleteNamespace deletes a Namespace and all of its Workflow Executions.
func DeleteNamespace(ctx context.Context, temporalClient client.Client, namespaceName string) error {
	// @@@SNIPSTART go-namespace-delete
	_, err := temporalClient.OperatorService().DeleteNamespace(ctx, &operatorservice.DeleteNamespaceRequest{
		Namespace: namespaceName,
	})
	// @@@SNIPEND
	return err
}
