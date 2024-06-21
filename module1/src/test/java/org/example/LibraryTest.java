package org.example;

import org.junit.Test;

import static org.junit.Assert.*;

public class LibraryTest {

    @Test
    public void test() throws InterruptedException {
        Library library = new Library();
        // we block the thread for 10 seconds to simulate a long-running operation
        Thread.sleep(60_000);
        assertEquals("Hello world!", library.getGreeting());
    }

}
