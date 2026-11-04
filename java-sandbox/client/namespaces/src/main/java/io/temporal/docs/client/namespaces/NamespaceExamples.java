package io.temporal.docs.client.namespaces;

import com.google.protobuf.util.Durations;
import io.temporal.api.namespace.v1.NamespaceConfig;
import io.temporal.api.namespace.v1.UpdateNamespaceInfo;
import io.temporal.api.operatorservice.v1.DeleteNamespaceRequest;
import io.temporal.api.operatorservice.v1.DeleteNamespaceResponse;
import io.temporal.api.workflowservice.v1.DeprecateNamespaceRequest;
import io.temporal.api.workflowservice.v1.DeprecateNamespaceResponse;
import io.temporal.api.workflowservice.v1.DescribeNamespaceRequest;
import io.temporal.api.workflowservice.v1.DescribeNamespaceResponse;
import io.temporal.api.workflowservice.v1.ListNamespacesRequest;
import io.temporal.api.workflowservice.v1.ListNamespacesResponse;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import io.temporal.api.workflowservice.v1.UpdateNamespaceRequest;
import io.temporal.api.workflowservice.v1.UpdateNamespaceResponse;
import io.temporal.serviceclient.OperatorServiceStubs;
import io.temporal.serviceclient.OperatorServiceStubsOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

/** Namespace management calls against a self-hosted Temporal Service. */
public class NamespaceExamples {

    // @@@SNIPSTART java-namespaces-register
    public static void createNamespace(WorkflowServiceStubs service, String name) {
        RegisterNamespaceRequest req =
            RegisterNamespaceRequest.newBuilder()
                .setNamespace(name)
                // Keeps the Workflow Execution Event History for up to 3 days in the Persistence store.
                // Not setting this value throws an error.
                .setWorkflowExecutionRetentionPeriod(Durations.fromDays(3))
                .build();
        service.blockingStub().registerNamespace(req);
    }
    // @@@SNIPEND

    public static UpdateNamespaceResponse updateNamespace(WorkflowServiceStubs service, String name) {
        // @@@SNIPSTART java-namespaces-update
        UpdateNamespaceRequest updateNamespaceRequest =
            UpdateNamespaceRequest.newBuilder()
                // The Namespace that you want to update
                .setNamespace(name)
                // Has options to update Namespace info
                .setUpdateInfo(
                    UpdateNamespaceInfo.newBuilder()
                        // Updates the description in the Namespace info
                        .setDescription("your updated namespace description")
                        .build())
                // Has options to update Namespace configuration
                .setConfig(
                    NamespaceConfig.newBuilder()
                        // Updates the Retention Period for the Namespace to 30 hours
                        .setWorkflowExecutionRetentionTtl(Durations.fromHours(30))
                        .build())
                .build();
        UpdateNamespaceResponse updateNamespaceResponse =
            service.blockingStub().updateNamespace(updateNamespaceRequest);
        // @@@SNIPEND
        return updateNamespaceResponse;
    }

    public static DescribeNamespaceResponse describeNamespace(
        WorkflowServiceStubs service, String name) {
        // @@@SNIPSTART java-namespaces-describe
        DescribeNamespaceRequest descNamespace =
            DescribeNamespaceRequest.newBuilder()
                // Specify the Namespace you want details for
                .setNamespace(name)
                .build();
        DescribeNamespaceResponse describeNamespaceResponse =
            service.blockingStub().describeNamespace(descNamespace);
        System.out.println("Namespace Description: " + describeNamespaceResponse);
        // @@@SNIPEND
        return describeNamespaceResponse;
    }

    public static ListNamespacesResponse listNamespaces(WorkflowServiceStubs service) {
        // @@@SNIPSTART java-namespaces-list
        ListNamespacesRequest listNamespaces = ListNamespacesRequest.newBuilder().build();
        // Lists one page of Namespaces. To list all of them, pass
        // getNextPageToken() back in the next request until it is empty.
        ListNamespacesResponse listNamespacesResponse =
            service.blockingStub().listNamespaces(listNamespaces);
        // @@@SNIPEND
        return listNamespacesResponse;
    }

    public static DeprecateNamespaceResponse deprecateNamespace(
        WorkflowServiceStubs service, String name) {
        // @@@SNIPSTART java-namespaces-deprecate
        DeprecateNamespaceRequest deprecateNamespace =
            DeprecateNamespaceRequest.newBuilder()
                // Specify the Namespace that you want to deprecate
                .setNamespace(name)
                .build();
        DeprecateNamespaceResponse response =
            service.blockingStub().deprecateNamespace(deprecateNamespace);
        // @@@SNIPEND
        return response;
    }

    public static DeleteNamespaceResponse deleteNamespace(WorkflowServiceStubs service, String name) {
        // @@@SNIPSTART java-namespaces-delete
        DeleteNamespaceResponse res =
            OperatorServiceStubs.newServiceStubs(
                    OperatorServiceStubsOptions.newBuilder()
                        .setChannel(service.getRawChannel())
                        .validateAndBuildWithDefaults())
                .blockingStub()
                .deleteNamespace(DeleteNamespaceRequest.newBuilder().setNamespace(name).build());
        // @@@SNIPEND
        return res;
    }
}
