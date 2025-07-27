package tecnico.communication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tecnico.utils.CollapsingSet;
import tecnico.configs.ClientConfig;
import tecnico.configs.NodeConfig;
import tecnico.configs.ProcessConfig;
import tecnico.encryption.EncryptionTools;

import tecnico.exceptions.ErrorMessage;
import tecnico.exceptions.HDSSException;
import tecnico.logger.CustomLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class AuthenticatedPerfectLink<T extends Message> {

    // Time to wait for an ACK before resending the message
    private static final long BASE_SLEEP_TIME = 1000;

    // UDP Socket for communication
    private final DatagramSocket socket;

    // Map of all nodes in the network
    private final Map<String, ProcessConfig> nodes = new ConcurrentHashMap<>();

    // Reference to the node itself
    private final ProcessConfig config;

    // Set of received messages from specific nodes (to prevent duplicates)
    private final Map<String, CollapsingSet> receivedMessages = new ConcurrentHashMap<>();

    // Set of received ACKs from specific nodes
    private final CollapsingSet receivedAcks = new CollapsingSet();

    // Message counter for unique message IDs
    private final AtomicInteger messageCounter = new AtomicInteger(0);

    // Queue to send messages to self (local delivery)
    private final Queue<T> localhostQueue = new ConcurrentLinkedQueue<>();

    // Logger for logging messages
    private final CustomLogger logger;

    // Gson instance for JSON serialization and deserialization
    private final Gson gson;

    // Class of the message type
    private final Class<T> messageClass;
    

    public AuthenticatedPerfectLink(ProcessConfig self, int port, ProcessConfig[] nodes, Class<T> messageClass) {
        try {
            this.config = self;
            this.logger = new CustomLogger(this.getClass().getName());
            this.messageClass = messageClass;

            // Initialize the nodes map and received messages set
            Arrays.stream(nodes).forEach(node -> {
                String id = node.getId();
                this.nodes.put(id, node);
                receivedMessages.put(id, new CollapsingSet());
            });

            // Initialize the UDP socket
            this.socket = new DatagramSocket(port, InetAddress.getByName(config.getHostname()));

            // Initialize Gson for JSON serialization/deserialization
            this.gson = new GsonBuilder().create();
        } catch (UnknownHostException | SocketException e) {
            throw new HDSSException(ErrorMessage.CANNOT_OPEN_SOCKET);
        }
    }

    public void ackAll(List<Integer> messageIds) {
        receivedAcks.addAll(messageIds);
    }

    public void broadcast(T data) {
        
        if (data.getType() != Message.Type.ACK)
            data.setMessageId(-1);
        
        // logger.info(MessageFormat.format("[BROADCASTING] - MESSAGE {0}", data));

        nodes.forEach((destId, dest) -> send(destId, data));
    }

    // Sends a message to a specific node with guaranteed delivery
    public void send(String nodeId, T message) {
        final T clonedMessage = (T) message.cloneMessage();
    
        new Thread(() -> {
            try {
                ProcessConfig node = nodes.get(nodeId);
                if (node == null)
                    throw new HDSSException(ErrorMessage.NO_SUCH_NODE);
    
                InetAddress destAddress = InetAddress.getByName(node.getHostname());

                int destPort = config instanceof ClientConfig
                        ? ((NodeConfig) node).getClientPort()
                        : node.getPort();
    
                if (clonedMessage.getType() != Message.Type.ACK)
                    clonedMessage.setMessageId(messageCounter.getAndIncrement());
                int messageId = clonedMessage.getMessageId();
    
                int count = 1;
                long sleepTime = BASE_SLEEP_TIME;
    
                String dataToSign = clonedMessage.toString(); 
                // System.out.println("[DEBUG] [SIGNING] [DATA] : " + dataToSign);
    
                try {
                    String signature = EncryptionTools.sign(dataToSign, config.getPrivateKeyPath());
                    clonedMessage.setSignature(signature);
                } catch (Exception e) {
                    logger.error(MessageFormat.format("[ERROR] Error signing message: {0}", e.getMessage()));
                    throw new HDSSException(ErrorMessage.GENERATING_SIGNATURE_ERROR);
                }
    
                // Serialize the message to JSON
                String jsonMessage = gson.toJson(clonedMessage);
                byte[] dataToSend = jsonMessage.getBytes();
    
                // Send to local queue if the destination is self
                if (nodeId.equals(this.config.getId())) {
                    this.localhostQueue.add(clonedMessage);
                    logger.info(MessageFormat.format("[SENT] {0} to \u001B[33mself (locally)\u001B[37m successfully", clonedMessage));
                    return;
                }
    
                // Send the message with retransmission
                while (true) {
                    // logger.info(MessageFormat.format("➡️ [SENDING] [DATA] {0} to {1}:{2} - \u001B[36mAttempt #{3}\u001B[37m", clonedMessage, destAddress, String.valueOf(destPort), count++));
    
                    unreliableSend(destAddress, destPort, dataToSend);
    
                    // Wait for ACK
                    Thread.sleep(sleepTime);
    
                    if (receivedAcks.contains(messageId)) {
                        // logger.info(MessageFormat.format("[ACK] Message {0} acknowledged by {1}:{2}", clonedMessage, destAddress, destPort));
                        break; // Stop retransmission after receiving ACK
                    }
    
                    sleepTime <<= 1; // Exponential backoff
                }
    
                // logger.info(MessageFormat.format("[SUCCESS] Message {0} received by {1}:{2} successfully", clonedMessage, destAddress, String.valueOf(destPort)));
            } catch (InterruptedException | UnknownHostException e) {
                logger.error(MessageFormat.format("[ERROR] Error sending message {0} to {1}: {2}", message, nodeId, e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    // Sends a message to a specific node without guaranteed delivery.
    public void unreliableSend(InetAddress hostname, int port, byte[] data) {
        new Thread(() -> {
            try {
                socket.send(new DatagramPacket(data, data.length, hostname, port));
            } catch (IOException e) {
                throw new HDSSException(ErrorMessage.SOCKET_SENDING_ERROR);
            }
        }).start();
    }

    // Receives a message from any node in the network (blocking).
    public T receive() throws IOException {
        T message;
        boolean local = false;
        DatagramPacket response = null;
    
        // Check if there is a message in the local queue
        if (!this.localhostQueue.isEmpty()) {
            message = this.localhostQueue.poll();
            local = true;
            this.receivedAcks.add(message.getMessageId()); // Add ACK to receivedAcks
        } else {
            // Receive a message from the network
            byte[] buf = new byte[65536];
            response = new DatagramPacket(buf, buf.length);
            socket.receive(response);
    
            // Deserialize the message from JSON
            byte[] buffer = Arrays.copyOfRange(response.getData(), 0, response.getLength());
            String jsonMessage = new String(buffer);
            message = gson.fromJson(jsonMessage, messageClass);
            // System.out.println("⬅️ [RECEIVED] [DATA] : " + message);
        }
    
        String senderId = message.getSenderId();
        int messageId = message.getMessageId();
    
        // Check if the sender is a known node
        if (!nodes.containsKey(senderId)) {
            throw new HDSSException(ErrorMessage.NO_SUCH_NODE);
        }

        if (message.getType().equals(Message.Type.ACK)) {
            receivedAcks.add(messageId);
            return message;
        }
    
        // System.out.println("[VERIFY] [SIGNATURE] [BASE64] : " + message.getSignature());
        // System.out.println("[PUBLIC KEY] [PATH] : " + nodes.get(senderId).getPublicKeyPath());
    
        // Validate the message signature
        boolean validSignature = EncryptionTools.verifySignature(
            message.toString(),
            message.getSignature(),
            nodes.get(senderId).getPublicKeyPath()
        );
    
        if (!validSignature) {
            logger.error(MessageFormat.format("⚠️ [ERROR] [INVALID SIGNATURE]: [MESSAGE] {0} [FROM] {1}", message, senderId));
            throw new HDSSException(ErrorMessage.INVALID_SIGNATURE_ERROR);
        }
    
        // Prevent duplicate messages
        boolean isRepeated = !receivedMessages.get(senderId).add(messageId);
        if (isRepeated) {
            logger.info(MessageFormat.format("⚠️ [ERROR] [DUPLICATE MESSAGE] : [MESSAGE] {0} [FROM] {1}", message, senderId));
            message.setType(Message.Type.IGNORE);
            return message; // Ignore duplicate messages
        }
    
        // Send an ACK if the message is not local and not an ACK itself
        if (!local && message.getType() != Message.Type.ACK) {
            InetAddress address = InetAddress.getByName(response.getAddress().getHostAddress());
            int port = response.getPort();
    
            // Create an ACK message
            Message responseMessage = new Message(this.config.getId(), Message.Type.ACK);
            responseMessage.setMessageId(messageId);
    
            try {
                // Sign the ACK message
                String dataToSign = responseMessage.toString();
                String signature = EncryptionTools.sign(dataToSign, config.getPrivateKeyPath());
                responseMessage.setSignature(signature);
            } catch (Exception e) {
                logger.error(MessageFormat.format("⚠️ [ERROR] Error signing ACK message: {0}", e.getMessage()));
                throw new HDSSException(ErrorMessage.GENERATING_SIGNATURE_ERROR);
            }
    
            // Serialize the ACK message to JSON
            String jsonResponse = gson.toJson(responseMessage);
            byte[] dataToSend = jsonResponse.getBytes();
    
            // Send the ACK message
            unreliableSend(address, port, dataToSend);
        }
    
        return message;
    }
}