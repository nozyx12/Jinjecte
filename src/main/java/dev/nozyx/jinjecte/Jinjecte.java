package dev.nozyx.jinjecte;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessHandle;
import java.util.*;

/**
 * Main CLI application for attaching to running Java Virtual Machines (JVMs),
 * retrieving system properties, and injecting Java agents.
 *
 * <p>This class provides an interactive console interface to:
 * <ul>
 *     <li>List active JVMs and attach to one</li>
 *     <li>Display JVM system information</li>
 *     <li>Read system properties of the attached JVM</li>
 *     <li>Inject Java agents into the attached JVM</li>
 * </ul>
 */
public class Jinjecte {
    private static final String VERSION = "1.0.0";
    private static final Scanner SCANNER = new Scanner(System.in);
    private static VirtualMachine currentVm;

    /**
     * Entry point of the Jinjecte CLI.
     * Lists JVMs, allows the user to attach, and launches the command loop.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        System.out.println("Jinjecte CLI | v" + VERSION);
        System.out.println("====================================\n");

        while (true) {
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();

            if (vms.isEmpty()) {
                System.out.println("[ERROR] No active JVM processes found.");
                return;
            }

            Map<Integer, VirtualMachineDescriptor> options = listJvms(vms);
            int choice = promptInt("Choose a JVM to attach to (0 to exit): ");

            if (choice == 0) break;

            if (!options.containsKey(choice)) {
                System.out.println("[ERROR] Invalid choice, please try again.");
                continue;
            }

            VirtualMachineDescriptor selectedVm = options.get(choice);
            if (attachToVm(selectedVm)) {
                commandLoop();
            }
        }

        SCANNER.close();
        System.out.println("Bye!");
    }

    /**
     * Lists all active JVMs on the system and prints them to the console.
     *
     * @param vms list of VirtualMachineDescriptor objects
     * @return a mapping of numeric selection index to JVM descriptor
     */
    private static Map<Integer, VirtualMachineDescriptor> listJvms(List<VirtualMachineDescriptor> vms) {
        long selfPid = ProcessHandle.current().pid();
        Map<Integer, VirtualMachineDescriptor> options = new LinkedHashMap<>();

        System.out.println("Active JVMs:");
        System.out.println("0. Exit");

        int index = 1;
        for (VirtualMachineDescriptor vm : vms) {
            String name = vm.displayName().isEmpty() ? "<unknown>" : vm.displayName();
            if (vm.id().equals(String.valueOf(selfPid))) {
                name = "(SELF) " + name;
            }
            System.out.printf("%d. PID: %s | Name: %s%n", index, vm.id(), name);
            options.put(index, vm);
            index++;
        }

        return options;
    }

    /**
     * Prompts the user for an integer input from the console.
     * Keeps asking until a valid number is entered.
     *
     * @param prompt message displayed to the user
     * @return integer value entered by the user
     */
    private static int promptInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid input, please enter a number.");
            }
        }
    }

    /**
     * Attaches to a JVM using the specified descriptor.
     *
     * @param vm the VirtualMachineDescriptor of the target JVM
     * @return true if attachment was successful, false otherwise
     */
    private static boolean attachToVm(VirtualMachineDescriptor vm) {
        try {
            System.out.println("\n[INFO] Attaching to JVM PID " + vm.id() + "...");
            currentVm = VirtualMachine.attach(vm);
            System.out.println("[SUCCESS] Connected to JVM " + vm.id() + "\n");
            return true;
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to attach to JVM: " + e.getMessage());
            return false;
        }
    }

    /**
     * Main command loop after a JVM is successfully attached.
     * Reads user input and executes commands: help, info, property, inject, exit.
     */
    private static void commandLoop() {
        while (true) {
            System.out.print("Jinjecte> ");
            String line = SCANNER.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase(Locale.ROOT);

            try {
                switch (cmd) {
                    case "help":
                        printHelp();
                        break;
                    case "info":
                        printVmInfo();
                        break;
                    case "property":
                        handlePropertyCommand(parts);
                        break;
                    case "inject":
                        handleInjectCommand(parts);
                        break;
                    case "exit":
                        detachVm();
                        return;
                    default:
                        System.out.println("[WARN] Unknown command: " + cmd + " (type 'help')");
                        break;
                }
            } catch (Throwable t) {
                System.out.println("[ERROR] Uncaught exception:");
                t.printStackTrace(System.err);
            }
        }
    }

    /**
     * Retrieves and prints a system property from the attached JVM.
     *
     * @param parts array where parts[1] is the property name
     */
    private static void handlePropertyCommand(String[] parts) {
        if (currentVm == null) {
            System.out.println("[WARN] No JVM is currently attached.");
            return;
        }

        if (parts.length < 2 || parts[1].isBlank()) {
            System.out.println("[WARN] No property name provided.");
            return;
        }

        String property = parts[1];
        try {
            Properties props = currentVm.getSystemProperties();
            System.out.println("> " + property + " = " + props.getOrDefault(property, "<unset>"));
        } catch (IOException e) {
            System.out.println("[ERROR] Could not fetch JVM properties: " + e.getMessage());
        }
    }

    /**
     * Injects a Java Agent into the currently attached JVM.
     *
     * @param parts array where parts[1] optionally specifies the path to the agent JAR
     */
    private static void handleInjectCommand(String[] parts) {
        if (currentVm == null) {
            System.out.println("[WARN] No JVM is currently attached.");
            return;
        }

        String agentPath;
        if (parts.length > 1 && !parts[1].isBlank()) {
            agentPath = parts[1];
        } else {
            System.out.print("Enter path to Java Agent JAR: ");
            agentPath = SCANNER.nextLine().trim();
        }

        if (agentPath.isEmpty()) {
            System.out.println("[WARN] No agent path provided.");
            return;
        }

        File agentFile = new File(agentPath);
        if (!agentFile.exists() || !agentFile.isFile()) {
            System.out.println("[ERROR] File does not exist: " + agentPath);
            return;
        }

        try {
            currentVm.loadAgent(agentFile.getAbsolutePath());
            System.out.println("[SUCCESS] Agent injected successfully: " + agentFile.getName());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to inject agent: " + e.getMessage());
        }
    }

    /**
     * Prints available CLI commands to the console.
     */
    private static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help                     : Show this help message");
        System.out.println("  info                     : Show info about the attached JVM");
        System.out.println("  property <property-name> : Get the value of a system property from the attached JVM");
        System.out.println("  inject <path-to-agent>   : Inject a Java Agent into the attached JVM");
        System.out.println("  exit                     : Detach from the JVM and return to main menu");
    }

    /**
     * Prints system properties and basic info of the attached JVM.
     */
    private static void printVmInfo() {
        if (currentVm == null) {
            System.out.println("[WARN] No JVM is currently attached.");
            return;
        }

        try {
            Properties props = currentVm.getSystemProperties();
            System.out.println("\nJVM Info:");
            System.out.println("  Java Version : " + props.getOrDefault("java.version", "<unset>"));
            System.out.println("  Java Vendor  : " + props.getOrDefault("java.vendor", "<unset>"));
            System.out.println("  Java Home    : " + props.getOrDefault("java.home", "<unset>"));
            System.out.println("  OS Name      : " + props.getOrDefault("os.name", "<unset>"));
            System.out.println("  OS Arch      : " + props.getOrDefault("os.arch", "<unset>"));
            System.out.println("  User Name    : " + props.getOrDefault("user.name", "<unset>"));
            System.out.println();
        } catch (IOException e) {
            System.out.println("[ERROR] Could not fetch JVM properties: " + e.getMessage());
        }
    }

    /**
     * Detaches from the currently attached JVM, releasing resources.
     */
    private static void detachVm() {
        if (currentVm == null) return;

        try {
            currentVm.detach();
            System.out.println("[INFO] Detached from JVM successfully.\n");
        } catch (IOException e) {
            System.out.println("[WARN] Could not detach gracefully: " + e.getMessage());
        } finally {
            currentVm = null;
        }
    }
}