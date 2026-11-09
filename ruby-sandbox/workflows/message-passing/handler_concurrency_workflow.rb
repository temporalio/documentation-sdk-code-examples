# frozen_string_literal: true

require 'temporalio/activity'
require 'temporalio/workflow'

class FetchDataActivity < Temporalio::Activity::Definition
  def execute
    @calls = (@calls || 0) + 1
    { 'x' => @calls, 'y' => @calls }
  end
end

# Defines its own MyWorkflow, so don't load it in the same process as the other MyWorkflow files in this folder.
# @@@SNIPSTART ruby-message-passing-handler-concurrency
class MyWorkflow < Temporalio::Workflow::Definition
  workflow_query_attr_reader :x, :y

  def execute
    Temporalio::Workflow.wait_condition { false }
  end

  workflow_signal
  def bad_handler
    data = Temporalio::Workflow.execute_activity(
      FetchDataActivity,
      start_to_close_timeout: 100
    )
    @x = data['x']
    # 🐛🐛 Bug!! If multiple instances of this handler are executing concurrently, then
    # there may be times when the Workflow has @x from one Activity execution and @y
    # from another.
    Temporalio::Workflow.sleep(1)
    @y = data['y']
  end

  workflow_signal
  def safe_handler
    @mutex ||= Temporalio::Workflow::Mutex.new
    @mutex.synchronize do
      data = Temporalio::Workflow.execute_activity(
        FetchDataActivity,
        start_to_close_timeout: 100
      )
      @x = data['x']
      # The mutex makes each handler instance finish setting @x and @y before the next
      # one starts, so @x and @y always come from the same Activity execution.
      Temporalio::Workflow.sleep(1)
      @y = data['y']
    end
  end
end
# @@@SNIPEND
