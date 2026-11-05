package io.temporal.docs.bestpractices.errorhandling;

import java.io.IOException;

// Stands in for a client library whose methods declare a checked exception.
public class GreetingService {
    public static String callExternalService(String greeting, String name) throws IOException {
        if (name.isEmpty()) {
            throw new IOException("Greeting service unavailable");
        }
        return greeting + ", " + name + "!";
    }
}
