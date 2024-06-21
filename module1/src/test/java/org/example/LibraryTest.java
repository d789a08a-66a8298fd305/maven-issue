package org.example;

public class LibraryTest extends AbstractGreetingTest{

    @Override
    IGreeting getGreetingImpl() {
        return new Library();
    }

}
