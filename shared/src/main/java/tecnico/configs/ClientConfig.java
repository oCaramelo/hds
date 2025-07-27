package tecnico.configs;

public class ClientConfig extends ProcessConfig {
    private String address;

    public ClientConfig() {}

    public ClientConfig(String id, String hostname, int port, String publicKeyPath, String privateKeyPath, String address, String behaviour) {
        super(id, hostname, port, publicKeyPath, privateKeyPath, Behaviour.valueOf(behaviour));
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}