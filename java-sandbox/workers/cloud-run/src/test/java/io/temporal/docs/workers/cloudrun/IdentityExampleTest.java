package io.temporal.docs.workers.cloudrun;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class IdentityExampleTest {
  @Test
  public void clientCreationFailsOutsideCloudRun() {
    assertThrows(IllegalStateException.class, IdentityExample::newClient);
  }
}
