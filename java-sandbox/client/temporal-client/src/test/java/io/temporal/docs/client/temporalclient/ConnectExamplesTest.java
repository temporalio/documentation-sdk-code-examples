package io.temporal.docs.client.temporalclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.grpc.Metadata;
import io.temporal.client.WorkflowClient;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.envconfig.LoadClientConfigProfileOptions;
import io.temporal.serviceclient.GrpcMetadataProvider;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConnectExamplesTest {

  private static final Metadata.Key<String> AUTHORIZATION =
      Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

  private static String authorization(WorkflowServiceStubsOptions options) {
    for (GrpcMetadataProvider provider : options.getGrpcMetadataProviders()) {
      String value = provider.getMetadata().get(AUTHORIZATION);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  @Test
  public void localServiceAndApiKeyClientsBuild() {
    assertNotNull(ConnectExamples.connectToLocalService());
    WorkflowClient cloud =
        ConnectExamples.connectWithApiKey("my-key", "localhost:7233", "ns.acct");
    assertEquals("ns.acct", cloud.getOptions().getNamespace());
  }

  @Test
  public void rotatedApiKeyIsSentWithSingleBearerPrefix() {
    WorkflowServiceStubsOptions options = ConnectExamples.rotateApiKey("old-key", "new-key");
    assertEquals("Bearer new-key", authorization(options));
  }

  private static Path[] selfSignedCert(Path dir) throws Exception {
    Path cert = dir.resolve("client.pem");
    Path key = dir.resolve("client.key");
    Process p;
    try {
      p =
          new ProcessBuilder(
                  "openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                  "-subj", "/CN=test", "-keyout", key.toString(), "-out", cert.toString())
              .redirectErrorStream(true)
              .start();
    } catch (java.io.IOException e) {
      assumeTrue(false, "openssl not available");
      return null;
    }
    assumeTrue(p.waitFor() == 0, "openssl failed");
    assumeTrue(Files.readString(key).contains("BEGIN PRIVATE KEY"), "key is not PKCS8");
    return new Path[] {cert, key};
  }

  @Test
  public void mtlsClientsBuild(@TempDir Path dir) throws Exception {
    Path[] files = selfSignedCert(dir);
    WorkflowClient client =
        ConnectExamples.connectWithMtls(
            files[0].toString(), files[1].toString(), "localhost:7233", "ns.acct");
    assertEquals("ns.acct", client.getOptions().getNamespace());
    assertNotNull(client.getWorkflowServiceStubs().getOptions().getSslContext());

    WorkflowServiceStubs rotating =
        ConnectExamples.rotatingMtlsStubs(files[0].toString(), files[1].toString(), "localhost:7233");
    assertNotNull(rotating.getOptions().getSslContext());
    rotating.shutdownNow();
  }

  private static String resource(String name) throws Exception {
    return Paths.get(ConnectExamplesTest.class.getResource(name).toURI()).toString();
  }

  private static ClientConfigProfile load(String file, String profile) throws Exception {
    return ClientConfigProfile.load(
        LoadClientConfigProfileOptions.newBuilder()
            .setConfigFilePath(resource(file))
            .setConfigFileProfile(profile)
            .setConfigFileStrict(true)
            .setEnvOverrides(java.util.Map.of())
            .build());
  }

  @Test
  public void configFileProfilesLoadStrictly() throws Exception {
    ClientConfigProfile dev = load("/config.toml", "default");
    assertEquals("localhost:7233", dev.getAddress());
    assertEquals(
        "development-value",
        dev.getMetadata().get(Metadata.Key.of("my-custom-header", Metadata.ASCII_STRING_MARSHALLER)));

    ClientConfigProfile prod = load("/config.toml", "prod");
    assertEquals("/etc/temporal/certs/client.pem", prod.getTls().getClientCertPath());

    ClientConfigProfile cloud = load("/config.toml", "cloud");
    assertEquals("your-api-key-here", cloud.getApiKey());
    assertTrue(cloud.toWorkflowServiceStubsOptions().getEnableHttps());
  }

  @Test
  public void mtlsProfileLoadsStrictly() throws Exception {
    ClientConfigProfile cloud = load("/cloud-mtls.toml", "cloud");
    assertEquals("/etc/temporal/certs/client.pem", cloud.getTls().getClientCertPath());
    assertEquals("/etc/temporal/certs/client.key", cloud.getTls().getClientKeyPath());
  }
}
