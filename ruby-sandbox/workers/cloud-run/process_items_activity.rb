# frozen_string_literal: true

require 'temporalio/activity'

# @@@SNIPSTART ruby-cloud-run-heartbeat
class ProcessItems < Temporalio::Activity::Definition
  def execute(items)
    # Resume from the item the last attempt was working on
    start = Temporalio::Activity::Context.current.info.heartbeat_details.first || 0
    items.each_with_index do |item, i|
      next if i < start

      Temporalio::Activity::Context.current.heartbeat(i)
      # ... process item
    end
    'done'
  end
end
# @@@SNIPEND
