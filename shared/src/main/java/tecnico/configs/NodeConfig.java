package tecnico.configs;

public class NodeConfig extends ProcessConfig {
    private boolean isLeader;
    private int clientPort;

    public NodeConfig() {}

    public NodeConfig(String id, String hostname, int port, String publicKeyPath, String privateKeyPath, String behaviour, boolean isLeader, int clientPort) {
        super(id, hostname, port, publicKeyPath, privateKeyPath, Behaviour.valueOf(behaviour));
        this.isLeader = isLeader;
        this.clientPort = clientPort;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }
}