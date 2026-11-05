package io.temporal.docs.bestpractices.testingsuite.nexus.handler;

import io.temporal.docs.bestpractices.testingsuite.nexus.service.SampleNexusService;

public interface EchoClient {
  SampleNexusService.EchoOutput echo(SampleNexusService.EchoInput input);
}
