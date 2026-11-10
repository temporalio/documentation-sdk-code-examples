# frozen_string_literal: true

require 'temporalio/error'
require 'temporalio/workflow'

# Calls SomethingThatFails from one of the Activity files in this folder.
# @@@SNIPSTART ruby-error-handling-fail-workflow
class SagaWorkflow < Temporalio::Workflow::Definition
  def execute(details)
    Temporalio::Workflow.execute_activity(SomethingThatFails, details, start_to_close_timeout: 30)
  rescue StandardError
    raise Temporalio::Error::ApplicationError.new('Fail the Workflow')
  end
end
# @@@SNIPEND
