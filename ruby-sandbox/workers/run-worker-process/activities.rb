# frozen_string_literal: true

require 'temporalio/activity'

class SayHello < Temporalio::Activity::Definition
  def execute(name)
    "Hello, #{name}!"
  end
end

# Registered as an instance so every execution shares one payment client.
# The payment client must be thread-safe.
class ChargeCard < Temporalio::Activity::Definition
  def initialize(payment_client)
    @payment_client = payment_client
  end

  def execute(order)
    @payment_client.charge(order['amount'])
  end
end
