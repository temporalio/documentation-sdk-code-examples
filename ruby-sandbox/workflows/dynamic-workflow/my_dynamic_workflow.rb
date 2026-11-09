# frozen_string_literal: true

require 'temporalio/error'
require 'temporalio/workflow'
require_relative 'my_activity'

# @@@SNIPSTART ruby-dynamic-workflow
class MyDynamicWorkflow < Temporalio::Workflow::Definition
  # Make this the dynamic workflow and accept raw args
  workflow_dynamic
  workflow_raw_args

  def execute(*raw_args)
    # Require a single arg for our workflow
    raise Temporalio::Error::ApplicationError, 'One arg expected' unless raw_args.size == 1

    # Use payload converter to convert it
    name = Temporalio::Workflow.payload_converter.from_payload(raw_args.first.payload)
    Temporalio::Workflow.execute_activity(
      MyActivity,
      { greeting: 'Hello', name: },
      start_to_close_timeout: 100
    )
  end
end
# @@@SNIPEND
