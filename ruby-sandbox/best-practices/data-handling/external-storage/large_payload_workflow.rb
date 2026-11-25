# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'large_payload_activity'

class LargePayloadWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    Temporalio::Workflow.execute_activity(LargePayloadActivity, input, start_to_close_timeout: 30)
  end
end
