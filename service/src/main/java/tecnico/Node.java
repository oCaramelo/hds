package tecnico;

import tecnico.communication.AuthenticatedPerfectLink;
import tecnico.communication.ConsensusMessage;
import tecnico.communication.LedgerMessage;
import tecnico.services.LedgerService;
import tecnico.services.NodeService;
import tecnico.configs.ProcessConfigBuilder;
import tecnico.configs.NodeConfig;
import tecnico.configs.ClientConfig;

import java.util.Arrays;


public class Node {

    private static void welcomeMessage(String serverId, NodeConfig nodeConfig) {
        System.out.println();
        System.out.println("╔══════════════════ Server Info ══════════════════╗");
        System.out.println("    Server ID is: " + serverId);
        System.out.println("    Hostname: " + nodeConfig.getHostname());
        System.out.println("    The application is now running on port: " + nodeConfig.getPort());
        System.out.println("    Leader status: " + nodeConfig.isLeader());
        System.out.println("    Behaviour: " + nodeConfig.getBehaviour());
        System.out.println("╚═════════════════════════════════════════════════╝");
    }

    private static String clientsConfigPath;
    private static String nodesConfigPath;

    public static void main(String[] args) {
        try {
            // Command line arguments
            String id = args[0];
            nodesConfigPath = args[1];
            clientsConfigPath = args[2];

            // Create configuration instances
            NodeConfig[] nodeConfigs = new ProcessConfigBuilder().fromNodesFile(nodesConfigPath);
            ClientConfig[] clientConfigs = new ProcessConfigBuilder().fromClientFile(clientsConfigPath);
            NodeConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();
            
            welcomeMessage(id, nodeConfig);

            // Abstraction to send and receive messages
            AuthenticatedPerfectLink<ConsensusMessage> authenticatedPerfectLinkToNodes = new AuthenticatedPerfectLink<>(
                nodeConfig, nodeConfig.getPort(), nodeConfigs, ConsensusMessage.class
            );

            AuthenticatedPerfectLink<LedgerMessage> authenticatedPerfectLinkToClients = new AuthenticatedPerfectLink<>(
                nodeConfig, nodeConfig.getClientPort(), clientConfigs, LedgerMessage.class
            );

            // Service to handle the node's logic - consensus
            NodeService nodeService = new NodeService(authenticatedPerfectLinkToNodes, authenticatedPerfectLinkToClients, nodeConfig, nodeConfigs);

            // Service to handle the node's logic - ledger
            LedgerService ledgerService = new LedgerService(authenticatedPerfectLinkToClients, nodeService);

            nodeService.listen();
            ledgerService.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
