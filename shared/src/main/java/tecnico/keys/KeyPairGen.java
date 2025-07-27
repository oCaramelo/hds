package tecnico.keys;

import java.io.*;
import java.security.*;
import java.nio.file.*;

public class KeyPairGen {
    public static void main(String[] args) {
        int numNodes = 4;
        int numClients = 2;

        try {
            for (int i = 1; i <= numNodes; i++) {
                String nodeDir = "src/main/java/tecnico/keys/keypairs/node_" + i;
                Path nodePath = Paths.get(nodeDir);
                if (!Files.exists(nodePath)) {
                    Files.createDirectories(nodePath);
                }

                KeyPair pair = generateKeyPair();

                saveKeyToFile(pair.getPrivate(), nodeDir + "/key.priv");
                saveKeyToFile(pair.getPublic(), nodeDir + "/key.pub");
            }

            for (int j = 1; j <= numClients; j++) {
                String clientDir = "src/main/java/tecnico/keys/keypairs/client_" + j;
                Path clientPath = Paths.get(clientDir);
                if (!Files.exists(clientPath)) {
                    Files.createDirectories(clientPath);
                }

                KeyPair pair = generateKeyPair();

                saveKeyToFile(pair.getPrivate(), clientDir + "/key.priv");
                saveKeyToFile(pair.getPublic(), clientDir + "/key.pub");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }


    private static void saveKeyToFile(Key key, String filename) throws IOException {
        byte[] encodedKey = key.getEncoded();
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(encodedKey);
        }
    }
}