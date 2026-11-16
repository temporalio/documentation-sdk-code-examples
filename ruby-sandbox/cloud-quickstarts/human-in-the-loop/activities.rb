# frozen_string_literal: true

require 'json'
require 'net/http'
require 'temporalio/activity'

module HumanInTheLoop
  # Asks the LLM for a proposed action and returns it as a ProposedAction Hash.
  #
  # If OPENAI_API_KEY isn't set, returns a mocked proposed action instead.
  class ProposeActionActivity < Temporalio::Activity::Definition
    RESPONSES_URL = URI('https://api.openai.com/v1/responses')

    # JSON schema for ProposedAction. Strict structured outputs require every
    # property to be listed as required and additionalProperties to be false.
    PROPOSED_ACTION_SCHEMA = {
      type: 'object',
      properties: {
        action_type: { type: 'string' },
        description: { type: 'string' },
        reasoning: { type: 'string' },
        risky_action: { type: 'boolean' }
      },
      required: %w[action_type description reasoning risky_action],
      additionalProperties: false
    }.freeze

    # A fixed, risky action used when no OpenAI API key is set, so the sample runs
    # end to end (including the approval step) without an LLM.
    def self.mock_proposed_action(user_request)
      {
        'action_type' => 'delete_test_data',
        'description' => "Mock action for the request: #{user_request}",
        'reasoning' => 'OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated.',
        'risky_action' => true
      }
    end

    # request is a Hash with 'model', 'instructions', and 'input'.
    def execute(request)
      api_key = ENV.fetch('OPENAI_API_KEY', '')
      if api_key.empty?
        Temporalio::Activity::Context.current.logger.warn(
          'OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI.'
        )
        return self.class.mock_proposed_action(request['input'])
      end

      text = create_response(api_key, request)

      # Parse here, not in the Workflow: if the output is unusable, this Activity
      # attempt fails and Temporal retries the LLM call. Raising in Workflow code
      # would instead fail the Workflow Task and replay the same bad result.
      action = begin
        JSON.parse(text.to_s)
      rescue JSON::ParserError
        nil
      end
      raise "LLM returned no parsable ProposedAction: #{text.inspect}" unless proposed_action?(action)

      action
    end

    private

    # Calls the OpenAI Responses API with structured outputs, so the response is
    # constrained to ProposedAction's JSON schema. Returns the output text.
    def create_response(api_key, request)
      body = {
        model: request['model'],
        instructions: request['instructions'],
        input: request['input'],
        text: {
          format: { type: 'json_schema', name: 'proposed_action', schema: PROPOSED_ACTION_SCHEMA, strict: true }
        }
      }
      http = Net::HTTP.new(RESPONSES_URL.host, RESPONSES_URL.port)
      http.use_ssl = true
      http.open_timeout = 10
      http.read_timeout = 30
      # Let Temporal own retries: don't let Net::HTTP retry on its own.
      http.max_retries = 0
      response = http.post(
        RESPONSES_URL.path, JSON.generate(body),
        'Authorization' => "Bearer #{api_key}", 'Content-Type' => 'application/json'
      )
      # Raising fails this attempt, so Temporal retries it.
      raise "OpenAI request failed with HTTP #{response.code}: #{response.body}" unless response.is_a?(Net::HTTPSuccess)

      output_text(JSON.parse(response.body))
    end

    # Concatenates the output_text parts of the response's message items.
    def output_text(response)
      response.fetch('output', [])
              .select { |item| item['type'] == 'message' }
              .flat_map { |item| item.fetch('content', []) }
              .select { |part| part['type'] == 'output_text' }
              .map { |part| part['text'] }
              .join
    end

    def proposed_action?(value)
      value.is_a?(Hash) &&
        %w[action_type description reasoning].all? { |key| value[key].is_a?(String) } &&
        [true, false].include?(value['risky_action'])
    end
  end

  # Tells a human that an action is waiting for approval.
  #
  # This sample prints to the Worker's terminal. A real system might send an email,
  # post to Slack, or update an approval queue UI instead.
  class NotifyApprovalNeededActivity < Temporalio::Activity::Definition
    def execute(request)
      context = Temporalio::Activity::Context.current
      workflow_id = context.info.workflow_id
      action = request['proposed_action']
      context.logger.info("Approval needed for request: #{request['request_id']}")

      line = '=' * 60
      puts <<~TEXT

        #{line}
        APPROVAL REQUIRED
        #{line}
        Workflow ID: #{workflow_id}
        Request ID:  #{request['request_id']}
        Action:      #{action['action_type']}
        Description: #{action['description']}
        Reasoning:   #{action['reasoning']}

        To approve or reject, run:
          bundle exec ruby send_approval.rb approve 'Looks good'
          bundle exec ruby send_approval.rb reject 'Too risky'

        Or with the Temporal CLI:
          temporal workflow signal --workflow-id #{workflow_id} --name approval_decision --input '{"approved": true}'
        #{line}

      TEXT
      $stdout.flush
      nil
    end
  end

  # Carries out the approved action.
  #
  # This sample only logs the action. A real system would call an external API,
  # change a database, or invoke a tool the LLM selected.
  class ExecuteActionActivity < Temporalio::Activity::Definition
    def execute(action)
      Temporalio::Activity::Context.current.logger.info("Executing action: #{action['action_type']}")
      "Successfully executed: #{action['action_type']}"
    end
  end
end
