package org.example;

import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationTest {

    @Test
    public void test() {
        assertEquals("\"Hello world!\" from Application!", Application.SINGLETON.getGreeting());
    }

}
