package org.example;

public class ApplicationTest extends AbstractGreetingTest {

    @Override
    IGreeting getGreetingImpl() {
        return Application.SINGLETON;
    }
}
