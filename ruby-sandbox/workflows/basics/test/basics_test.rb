# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'time'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../my_workflow'
require_relative '../custom_name_workflow'
require_relative '../workflow_init_workflow'
require_relative '../logging_workflow'
require_relative '../determinism_workflow'

class BasicsTest < Minitest::Test
  
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  # Define::MyWorkflow and Logging::MyWorkflow share the default name MyWorkflow, so each test registers only the
  # Workflow it runs.
  def with_worker(workflow, &)
    worker = Temporalio::Worker.new(
      client: self.class.env.client,
      task_queue: @task_queue = "workflow-basics-#{SecureRandom.uuid}",
      workflows: [workflow],
      activities: [MyActivity]
    )
    worker.run(&)
  end

  def execute(workflow, *args)
    self.class.env.client.execute_workflow(workflow, *args, id: "wf-#{SecureRandom.uuid}", task_queue: @task_queue)
  end

  def test_define
    with_worker(Define::MyWorkflow) { assert_equal 'Hello, Temporal!', execute(Define::MyWorkflow, 'Temporal') }
  end

  def test_default_name_is_unqualified_class_name
    assert_equal 'MyWorkflow', Temporalio::Workflow::Definition::Info.from_class(Define::MyWorkflow).name
  end

  def test_custom_name
    assert_equal 'MyDifferentWorkflowName',
                 Temporalio::Workflow::Definition::Info.from_class(CustomName::MyWorkflow).name
    with_worker(CustomName::MyWorkflow) do
      assert_equal 'Hello, Temporal!', execute('MyDifferentWorkflowName', 'Temporal')
    end
  end

  def test_workflow_init
    with_worker(WorkflowInitWorkflow) do
      handle = self.class.env.client.start_workflow(
        WorkflowInitWorkflow, { 'name' => 'Temporal' }, id: "wf-#{SecureRandom.uuid}", task_queue: @task_queue
      )
      assert_equal 'Knight Temporal', handle.execute_update(WorkflowInitWorkflow.check_title)
      assert_equal 'Hello, Knight Temporal', handle.result
    end
  end

  # The Workflow logs through Temporalio::Workflow.logger; completing shows Logger isn't flagged as an illegal call.
  def test_logging
    with_worker(Logging::MyWorkflow) { execute(Logging::MyWorkflow, 'Temporal') }
  end

  def test_determinism_alternatives
    with_worker(DeterminismWorkflow) do
      result = execute(DeterminismWorkflow)
      assert_includes 1..100, result['value']
      assert_match(/\A\h{8}-\h{4}-\h{4}-\h{4}-\h{12}\z/, result['unique_id'])
      refute_nil Time.iso8601(result['current_time'])
    end
  end
end
