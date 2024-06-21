package org.example;

import static org.junit.Assert.assertTrue;

public abstract class AbstractGreetingTest {

    abstract IGreeting getGreetingImpl();

    public void test() {
        assertTrue(getGreetingImpl().getGreeting().contains("Hello world"));
    }


}
