package tecnico.exceptions;

public enum ErrorMessage {
    CONFIG_FILE_NOT_FOUND("The configuration file is not available at the path supplied"),
    CONFIG_FILE_FORMAT("The configuration file has wrong syntax"),
    NO_SUCH_NODE("Can't send a message to a non existing node"),
    SOCKET_SENDING_ERROR("Error while sending message"),
    CANNOT_OPEN_SOCKET("Error while opening socket"),
    INVALID_SIGNATURE_ERROR("The signature is invalid"),
    GENERATING_SIGNATURE_ERROR("Error while generating the signature for the message"),
    VERIFYING_SIGNATURE_ERROR("Error while verifying the signature of the received message"),
    ERROR_SIGNING_DATA("Error while try to sign data"),
    INVALID_CONFIG_TYPE("Config type is neither Node nor Client");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}