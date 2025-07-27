package tecnico.configs;

public class ProcessConfig {
    private String id;
    private String hostname;
    private int port;
    private String publicKeyPath;
    private String privateKeyPath;
    private Behaviour behaviour = Behaviour.NONE;

    public ProcessConfig() {}

    public ProcessConfig(String id, String hostname, int port, String publicKeyPath, String privateKeyPath, Behaviour behaviour) {
        this.id = id;
        this.hostname = hostname;
        this.port = port;
        this.publicKeyPath = publicKeyPath;
        this.privateKeyPath = privateKeyPath;
        this.behaviour = behaviour;
    }

    public enum Behaviour {
        NONE("NONE"),        
        DROP("DROP"),       
        INVALID_SIGNATURE("INVALID_SIGNATURE"),
        WRONG_BLOCK("WRONG_BLOCK"),
        DELAY("DELAY");

        private final String behaviour;

        Behaviour(String behaviour) {
            this.behaviour = behaviour;
        }

        public String getBehaviour() {
            return behaviour;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

    public void setBehaviour(Behaviour behaviour) {
        this.behaviour = behaviour;
    }
}