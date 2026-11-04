package io.temporal.docs.workers.awslambda;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/** Loads each Lambda entry point so its static Worker definition runs. */
public class HandlersTest {

  @Test
  public void lambdaFunctionDefinesWorker() {
    assertDoesNotThrow(() -> Class.forName(LambdaFunction.class.getName()));
  }

  @Test
  public void otelHandlerDefinesWorker() {
    assertDoesNotThrow(() -> Class.forName(Handler.class.getName()));
  }
}
