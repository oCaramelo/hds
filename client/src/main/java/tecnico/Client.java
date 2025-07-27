package tecnico;

import java.util.Arrays;
import java.util.Scanner;
import java.math.BigInteger;
import tecnico.configs.ClientConfig;
import tecnico.configs.NodeConfig;
import tecnico.configs.ProcessConfig;
import tecnico.configs.ProcessConfigBuilder;

public class Client {

    private static String clientsConfigPath;
    private static String nodesConfigPath;

    private static void menuMessage(String clientId, ProcessConfig clientConfig) {
        System.out.println();
        System.out.println();
        System.out.println("    ▓█████▄ ▓█████  ██▓███   ▄████▄   ██░ ██  ▄▄▄       ██▓ ███▄    █ ");
        System.out.println("    ▒██▀ ██▌▓█   ▀ ▓██░  ██▒▒██▀ ▀█  ▓██░ ██▒▒████▄    ▓██▒ ██ ▀█   █ ");
        System.out.println("    ░██   █▌▒███   ▓██░ ██▓▒▒▓█    ▄ ▒██▀▀██░▒██  ▀█▄  ▒██▒▓██  ▀█ ██▒");
        System.out.println("    ░▓█▄   ▌▒▓█  ▄ ▒██▄█▓▒ ▒▒▓▓▄ ▄██▒░▓█ ░██ ░██▄▄▄▄██ ░██░▓██▒  ▐▌██▒");
        System.out.println("    ░▒████▓ ░▒████▒▒██▒ ░  ░▒ ▓███▀ ░░▓█▒░██▓ ▓█   ▓██▒░██░▒██░   ▓██░");
        System.out.println("     ▒▒▓  ▒ ░░ ▒░ ░▒▓▒░ ░  ░░ ░▒ ▒  ░ ▒ ░░▒░▒ ▒▒   ▓▒█░░▓  ░ ▒░   ▒ ▒ ");
        System.out.println("     ░ ▒  ▒  ░ ░  ░░▒ ░       ░  ▒    ▒ ░▒░ ░  ▒   ▒▒ ░ ▒ ░░ ░░   ░ ▒░");
        System.out.println("     ░ ░  ░    ░   ░░       ░         ░  ░░ ░  ░   ▒    ▒ ░   ░   ░ ░ ");
        System.out.println("       ░       ░  ░         ░ ░       ░  ░  ░      ░  ░ ░           ░ ");
        System.out.println("     ░                      ░                                        ");
        System.out.println();

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.printf ("║ %-70s ║\n", "");
        System.out.printf ("║ %-70s ║\n", "                ███╗   ███╗███████╗███╗   ██╗██╗   ██╗");
        System.out.printf ("║ %-70s ║\n", "                ████╗ ████║██╔════╝████╗  ██║██║   ██║");
        System.out.printf ("║ %-70s ║\n", "                ██╔████╔██║█████╗  ██╔██╗ ██║██║   ██║");
        System.out.printf ("║ %-70s ║\n", "                ██║╚██╔╝██║██╔══╝  ██║╚██╗██║██║   ██║");
        System.out.printf ("║ %-70s ║\n", "                ██║ ╚═╝ ██║███████╗██║ ╚████║╚██████╔╝");
        System.out.printf ("║ %-70s ║\n", "                ╚═╝     ╚═╝╚══════╝╚═╝  ╚═══╝ ╚═════╝");
        System.out.printf ("║ %-70s ║\n", "");
        System.out.println("╠════════════════════════════════════════════════════════════════════════╣");
        System.out.printf ("║ %-70s ║\n", "Your client ID is: " + clientId);
        System.out.printf ("║ %-70s ║\n", "The application is now running on port: " + clientConfig.getPort());
        System.out.printf ("║ %-70s ║\n", "");
        System.out.printf ("║ %-70s ║\n", "Command options:");
        System.out.printf ("║ %-70s ║\n", "- 'SHOW_PROFILE' : Display your account profile");
        System.out.printf ("║ %-70s ║\n", "- 'TRANSFER <to> <value> [ISTCoin]' : Transfer DepCoin or ISTCoin");
        System.out.printf ("║ %-70s ║\n", "- 'SHOW_BLOCKCHAIN' : Display the blockchain");
        System.out.printf ("║ %-70s ║\n", "- 'SHOW_NETWORK' : Display all users addresses in the network");
        System.out.printf ("║ %-70s ║\n", "- 'SHOW_MENU' : Display menu");
        System.out.printf ("║ %-70s ║\n", "- 'EXIT' : Exit the program");
        System.out.println ("╚════════════════════════════════════════════════════════════════════════╝");
    }

    public static void sleep(int seconds) {
        try {
            long milliseconds = seconds * 1000;
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            String id = args[0];
            nodesConfigPath = args[1];
            clientsConfigPath = args[2];

            int baseTimeSleep = 2;

            // Create configuration instances
            ClientConfig[] clientConfigs = new ProcessConfigBuilder().fromClientFile(clientsConfigPath);
            NodeConfig[] nodeConfigs = new ProcessConfigBuilder().fromNodesFile(nodesConfigPath);

            ClientConfig clientConfig = Arrays.stream(clientConfigs).filter(c -> c.getId().equals(id)).findAny().get();
            String clientAddress = clientConfig.getAddress();


            for (NodeConfig nodeConfig : nodeConfigs) {
                nodeConfig.setPort(nodeConfig.getClientPort());
            }

            menuMessage(id, clientConfig);

            final ClientLibrary service = new ClientLibrary(clientConfig, nodeConfigs, clientConfigs);
            service.listen();

            String line = "";
            String prompt = String.format("🔗 [DepChain] User @ %s » ", id);

            while (true) {
                System.out.flush();
                System.out.println();
                System.out.print(prompt);
                line = scanner.nextLine();
                System.out.println();

                if ((line = line.trim()).length() == 0) {
                    continue;
                }

                String[] tokens = line.split(" ");

                switch (tokens[0].toUpperCase()) {
                    case "TRANSFER" -> {
                        if (tokens.length < 4 || tokens.length > 4) {
                            System.out.println("[CLIENT_NODE] [HELP] : TRANSFER <to> <value> [ISTCoin]");
                            continue;
                        }
                        try {
                            String to = tokens[1];
                            BigInteger value = new BigInteger(tokens[2]);
                            boolean isISTCoin = tokens.length == 4 && tokens[3].equalsIgnoreCase("ISTCoin");
                            // System.out.println("[CLIENT_NODE] [ACTION] : TRANSFER " + (isISTCoin ? "IST Coin" : "Dep Coin"));
                            service.transfer(clientAddress, to, value, isISTCoin);
                            sleep(baseTimeSleep);
                        } catch (NumberFormatException e) {
                            System.out.println("[CLIENT_NODE] [ERROR] : Invalid value format");
                        }
                    }
                    case "SHOW_PROFILE" -> {
                        if (tokens.length != 1) {
                            System.out.println("[CLIENT_NODE] [HELP] : SHOW_PROFILE");
                            continue;
                        }
                        // System.out.println("[CLIENT_NODE] [ACTION] : SHOW PROFILE");
                        service.showProfile(clientAddress);
                        sleep(baseTimeSleep);
                    }
                    case "SHOW_BLOCKCHAIN" -> {
                        if (tokens.length != 1) {
                            System.out.println("[CLIENT_NODE] [HELP] : SHOW_BLOCKCHAIN");
                            continue;
                        }
                        // System.out.println("[CLIENT_NODE] [ACTION] : SHOW BLOCKCHAIN");
                        service.showBlockchain(clientAddress);
                        sleep(baseTimeSleep);
                    }
                    case "SHOW_NETWORK" -> {
                        if (tokens.length != 1) {
                            System.out.println("[CLIENT_NODE] [HELP] : SHOW_NETWORK");
                            continue;
                        }
                        // System.out.println("[CLIENT_NODE] [ACTION] : SHOW NETWORK");
                        service.showNetwork(clientAddress);
                        sleep(baseTimeSleep);
                    }
                    case "SHOW_MENU" -> {
                        if (tokens.length != 1) {
                            System.out.println("[CLIENT_NODE] [HELP] : SHOW_MENU");
                            continue;
                        }
                        // System.out.println("[CLIENT_NODE] [ACTION] : SHOW MENU");
                        menuMessage(id, clientConfig);
                    }
                    case "EXIT" -> {
                        System.out.println("[CLIENT_NODE] [ACTION] : EXITING...");
                        scanner.close();
                        System.exit(0);
                    }
                    default -> {
                        System.out.println("[CLIENT_NODE] [ALERT] : INVALID COMMAND!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
