from temporalio import activity


@activity.defn
async def validate_order_activity(order_id: str) -> None:
    activity.logger.info("Validating order %s", order_id)


@activity.defn
async def charge_payment_activity(order_id: str) -> None:
    activity.logger.info("Charging payment for order %s", order_id)


@activity.defn
async def dispatch_order_activity(order_id: str) -> str:
    return f"order {order_id} shipped"
