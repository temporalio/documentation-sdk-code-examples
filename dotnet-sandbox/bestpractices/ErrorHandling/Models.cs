namespace TemporalioDocs.BestPractices.ErrorHandling;

public record Bill(string CustomerId, string Department, decimal Amount);

public record OrderConfirmation(string OrderId);

public record Customer(string Name, string CreditCardNumber);

public record Order(string OrderId, Customer Customer);
