package dev.nozyx.jinjecte;

/**
 * Wrapper class to ensure the required attach API classes are available.
 *
 * <p>This class checks that the JDK's attach tools classes are present:
 * {@link com.sun.tools.attach.VirtualMachine} and {@link com.sun.tools.attach.VirtualMachineDescriptor}.
 * If not found, it prints a helpful message and exits.
 *
 * <p>Delegates execution to {@link Jinjecte#main(String[])} after validation.
 */
public class Wrapper {
    /**
     * Entry point for the wrapper.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            Class.forName("com.sun.tools.attach.VirtualMachineDescriptor");
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found: " + e.getMessage());
            System.out.println("Please make sure you are running this program using a JDK with attach tools!");
            System.exit(1);
        }

        Jinjecte.main(args);
    }
}