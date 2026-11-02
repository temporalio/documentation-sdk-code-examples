export interface PizzaOrder {
  orderNumber: string;
  pizzas: string[];
}

export interface OrderConfirmation {
  orderNumber: string;
  status: string;
}

// @@@SNIPSTART typescript-versioning-cutover-workflows
export async function pizzaWorkflow(order: PizzaOrder): Promise<OrderConfirmation> {
  // this function contains the original code
  return { orderNumber: order.orderNumber, status: 'SUCCESS' };
}

export async function pizzaWorkflowV2(order: PizzaOrder): Promise<OrderConfirmation> {
  // this function contains the updated code
  const status = order.pizzas.length > 0 ? 'SUCCESS' : 'EMPTY_ORDER';
  return { orderNumber: order.orderNumber, status };
}
// @@@SNIPEND
