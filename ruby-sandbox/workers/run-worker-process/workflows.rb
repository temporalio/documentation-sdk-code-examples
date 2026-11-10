# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'activities'

class GreetingWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    Temporalio::Workflow.execute_activity(SayHello, name, start_to_close_timeout: 10)
  end
end

class OrderWorkflow < Temporalio::Workflow::Definition
  def execute(order)
    Temporalio::Workflow.execute_activity(ChargeCard, order, start_to_close_timeout: 10)
  end
end

class VersionedGreetingWorkflow < Temporalio::Workflow::Definition
  workflow_versioning_behavior Temporalio::VersioningBehavior::PINNED

  def execute(name)
    Temporalio::Workflow.execute_activity(SayHello, name, start_to_close_timeout: 10)
  end
end
