package io.temporal.docs.workflows.workflowstreams.llm;

// @@@SNIPSTART java-workflow-streams-llm-activity
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import io.temporal.activity.Activity;
import io.temporal.workflowstreams.TopicHandle;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamClientOptions;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

class TextDelta {
  public String text;

  public TextDelta() {}

  public TextDelta(String text) {
    this.text = text;
  }
}

class RetryEvent {
  public int attempt;

  public RetryEvent() {}

  public RetryEvent(int attempt) {
    this.attempt = attempt;
  }
}

// Jackson can't serialize a class with no properties, so give the sentinel one.
class CloseEvent {
  public boolean done = true;
}

public class LlmActivitiesImpl implements LlmActivities {
  @Override
  public String streamCompletion(String prompt) {
    WorkflowStreamClientOptions options =
        WorkflowStreamClientOptions.newBuilder()
            .setBatchInterval(Duration.ofMillis(200))
            .build();
    try (WorkflowStreamClient streamClient = WorkflowStreamClient.fromActivity(options)) {
      TopicHandle deltas = streamClient.topic("delta");
      TopicHandle retry = streamClient.topic("retry");
      TopicHandle close = streamClient.topic("close");

      // Tell consumers an earlier attempt's deltas are stale.
      int attempt = Activity.getExecutionContext().getInfo().getAttempt();
      if (attempt > 1) {
        retry.publish(new RetryEvent(attempt), /* forceFlush */ true);
      }

      // Disable provider-side retries; let Temporal own retry policy at the Activity layer.
      OpenAIClient openai = OpenAIOkHttpClient.builder().fromEnv().maxRetries(0).build();
      ChatCompletionCreateParams params =
          ChatCompletionCreateParams.builder().model("gpt-4o-mini").addUserMessage(prompt).build();

      StringBuilder full = new StringBuilder();
      AtomicBoolean first = new AtomicBoolean(true);
      try (StreamResponse<ChatCompletionChunk> stream =
          openai.chat().completions().createStreaming(params)) {
        stream.stream()
            .forEach(
                chunk ->
                    chunk.choices().stream()
                        .findFirst()
                        .flatMap(choice -> choice.delta().content())
                        .filter(text -> !text.isEmpty())
                        .ifPresent(
                            text -> {
                              // forceFlush only on the first delta so the user sees something
                              // immediately; subsequent deltas batch at the 200 ms interval.
                              deltas.publish(new TextDelta(text), first.getAndSet(false));
                              full.append(text);
                            }));
      }
      close.publish(new CloseEvent());
      return full.toString();
    }
  }
}
// @@@SNIPEND
