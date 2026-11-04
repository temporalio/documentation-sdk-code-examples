package io.temporal.docs.nexus.featureguide.messaging;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;

@Service
public interface GreetingNexusService {
  class ApproveInput {
    private final String userId;
    private final String name;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ApproveInput(@JsonProperty("userId") String userId, @JsonProperty("name") String name) {
      this.userId = userId;
      this.name = name;
    }

    @JsonProperty("userId")
    public String getUserId() {
      return userId;
    }

    @JsonProperty("name")
    public String getName() {
      return name;
    }
  }

  // A class-level Jackson annotation lets Jackson serialize this empty type as {}.
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  class ApproveOutput {
    @JsonCreator
    public ApproveOutput() {}
  }

  @Operation
  ApproveOutput approve(ApproveInput input);
}
