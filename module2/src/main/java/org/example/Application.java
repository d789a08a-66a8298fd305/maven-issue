package org.example;

public class Application implements IGreeting {
    public static final Application SINGLETON = new Application();
    private final Library library = new Library();


    public static void main(String[] args) {
        System.out.println(SINGLETON.getGreeting());
    }

    @Override
    public String getGreeting() {
        return "\"" + library.getGreeting() + "\" from Application!";
    }
}
