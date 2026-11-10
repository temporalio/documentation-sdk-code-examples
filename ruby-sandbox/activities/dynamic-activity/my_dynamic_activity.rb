# frozen_string_literal: true

require 'temporalio/activity'
require 'temporalio/error'

# @@@SNIPSTART ruby-dynamic-activity
class MyDynamicActivity < Temporalio::Activity::Definition
  # Make this the dynamic activity and accept raw args
  activity_dynamic
  activity_raw_args

  def execute(*raw_args)
    raise Temporalio::Error::ApplicationError, 'One arg expected' unless raw_args.size == 1

    # Use payload converter to convert it
    input = Temporalio::Activity::Context.current.payload_converter.from_payload(raw_args.first.payload)
    "#{input['greeting']}, #{input['name']}!"
  end
end
# @@@SNIPEND
