# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../dynamic_handlers_workflow'

class DynamicHandlersTest < MessagePassingTest
  def test_dynamic_handlers
    with_worker(workflows: [DynamicHandlersWorkflow]) do
      handle = start(DynamicHandlersWorkflow)
      assert_equal 'Got parameter foo for query any-query', handle.query('any-query', 'foo')

      handle.signal('any-signal', 'bar')
      handle.execute_update('any-update', 'baz')
      assert_equal ['Got parameter bar for signal any-signal', 'Got parameter baz for update any-update'],
                   handle.result
    end
  end
end
