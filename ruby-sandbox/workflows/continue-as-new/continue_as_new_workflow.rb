# frozen_string_literal: true

require 'temporalio/workflow'

# Continues-As-New once. The new run gets 'my-new-arg' as its input and completes.
class ContinueAsNewWorkflow < Temporalio::Workflow::Definition
  def execute(arg)
    return "Finished with #{arg}" if arg == 'my-new-arg'

    # @@@SNIPSTART ruby-continue-as-new
    raise Temporalio::Workflow::ContinueAsNewError.new('my-new-arg')
    # @@@SNIPEND
  end
end
