# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'activities'

# Runs three Activities at once and waits for all of them.
class AllOfWorkflow < Temporalio::Workflow::Definition
  def execute
    # @@@SNIPSTART ruby-futures-all-of
    # Start 3 activities in background
    fut1 = Temporalio::Workflow::Future.new do
      Temporalio::Workflow.execute_activity(MyActivity1, schedule_to_close_timeout: 300)
    end
    fut2 = Temporalio::Workflow::Future.new do
      Temporalio::Workflow.execute_activity(MyActivity2, schedule_to_close_timeout: 300)
    end
    fut3 = Temporalio::Workflow::Future.new do
      Temporalio::Workflow.execute_activity(MyActivity3, schedule_to_close_timeout: 300)
    end

    # Wait for them all to complete
    Temporalio::Workflow::Future.all_of(fut1, fut2, fut3).wait

    Temporalio::Workflow.logger.info("Got: #{fut1.result}, #{fut2.result}, #{fut3.result}")
    # @@@SNIPEND
    [fut1.result, fut2.result, fut3.result]
  end
end
