package io.temporal.docs.client.temporalclient;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.util.AdvancedTlsX509KeyManager;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Ways to create a Temporal Client for a local Temporal Service or Temporal Cloud. */
public class ConnectExamples {

  public static WorkflowClient connectToLocalService() {
    // @@@SNIPSTART java-temporal-client-local-service
    // Create an instance that connects to a Temporal Service running on the local machine, using the default port 7233
    WorkflowServiceStubs serviceStub = WorkflowServiceStubs.newLocalServiceStubs();

    // Initialize the Client
    WorkflowClient client = WorkflowClient.newInstance(serviceStub);
    // @@@SNIPEND
    return client;
  }

  public static WorkflowClient connectToNamespace(WorkflowServiceStubs service, String namespace) {
    // @@@SNIPSTART java-temporal-client-custom-namespace
    // Add the Namespace as a Client Option
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setNamespace(namespace).build();

    // Initialize the Client
    WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
    // @@@SNIPEND
    return client;
  }

  public static WorkflowClient connectWithApiKey(String apiKey, String endpoint, String namespace) {
    // @@@SNIPSTART java-temporal-client-api-key
    WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .addApiKey(() -> apiKey)
                .setTarget(endpoint)
                .setEnableHttps(true)
                .build());

    // namespace is your <namespace_id>.<account_id>
    WorkflowClient client =
        WorkflowClient.newInstance(
            service, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    // @@@SNIPEND
    return client;
  }

  public static WorkflowServiceStubsOptions rotateApiKey(String apiKey, String newApiKey) {
    // @@@SNIPSTART java-temporal-client-update-api-key
    AtomicReference<String> myKey = new AtomicReference<>(apiKey);
    WorkflowServiceStubsOptions stubOptions =
        WorkflowServiceStubsOptions.newBuilder().addApiKey(myKey::get).build();
    // The SDK calls the supplier on each request and adds the "Bearer " prefix.
    // Replace the key atomically; later requests use the new value.
    myKey.set(newApiKey);
    // @@@SNIPEND
    return stubOptions;
  }

  public static WorkflowServiceStubs mtlsStubs(SslContext sslContext, String gRPCEndpoint) {
    // @@@SNIPSTART java-temporal-client-mtls-stubs
    // Set the Service Stub options (SSL context and gRPC endpoint)
    WorkflowServiceStubsOptions stubsOptions =
        WorkflowServiceStubsOptions.newBuilder()
            .setSslContext(sslContext)
            .setTarget(gRPCEndpoint)
            .build();

    // Create a stub that accesses a Temporal Service
    WorkflowServiceStubs serviceStub = WorkflowServiceStubs.newServiceStubs(stubsOptions);
    // @@@SNIPEND
    return serviceStub;
  }

  public static WorkflowClient connectWithMtls(
      String clientCertPath, String clientKeyPath, String gRPCEndpoint, String namespace)
      throws IOException {
    // @@@SNIPSTART java-temporal-client-mtls-client
    // Generate an SSL context
    InputStream clientCertInputStream = new FileInputStream(clientCertPath);
    InputStream clientKeyInputStream = new FileInputStream(clientKeyPath);
    SslContext sslContext =
        SimpleSslContextBuilder.forPKCS8(clientCertInputStream, clientKeyInputStream).build();

    // Set the Service Stub options (SSL context and gRPC endpoint)
    WorkflowServiceStubsOptions stubsOptions =
        WorkflowServiceStubsOptions.newBuilder()
            .setSslContext(sslContext)
            .setTarget(gRPCEndpoint)
            .build();

    // Create a stub that accesses a Temporal Service
    WorkflowServiceStubs serviceStub = WorkflowServiceStubs.newServiceStubs(stubsOptions);

    // Set the Client options
    WorkflowClientOptions clientOptions =
        WorkflowClientOptions.newBuilder().setNamespace(namespace).build();

    // Initialize the Client
    WorkflowClient client = WorkflowClient.newInstance(serviceStub, clientOptions);
    // @@@SNIPEND
    return client;
  }

  public static WorkflowServiceStubs rotatingMtlsStubs(
      String tlsCertPath, String tlsKeyPath, String gRPCEndpoint)
      throws IOException, GeneralSecurityException {
    // @@@SNIPSTART java-temporal-client-mtls-rotation
    // tlsKeyPath must point to a PKCS8 key
    // Reread the certificate and key files every 5 minutes.
    AdvancedTlsX509KeyManager keyManager = new AdvancedTlsX509KeyManager();
    keyManager.updateIdentityCredentialsFromFile(
        new File(tlsKeyPath),
        new File(tlsCertPath),
        5,
        TimeUnit.MINUTES,
        Executors.newSingleThreadScheduledExecutor());

    SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
    GrpcSslContexts.configure(sslContextBuilder);
    SslContext sslContext = sslContextBuilder.keyManager(keyManager).build();

    WorkflowServiceStubsOptions stubsOptions =
        WorkflowServiceStubsOptions.newBuilder()
            .setSslContext(sslContext)
            .setTarget(gRPCEndpoint)
            .build();
    WorkflowServiceStubs serviceStub = WorkflowServiceStubs.newServiceStubs(stubsOptions);
    // @@@SNIPEND
    return serviceStub;
  }
}
