package tecnico.utils;

import tecnico.models.Block;
import tecnico.models.Transaction;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ByzantineUtils {

    public static Block generateRandomBlock() {

        Transaction fakeTx = new Transaction(
            randomId(),                       
            randomId(),                      
            new BigInteger("999999"),      
            "malicious_input_data",          
            "ISTCoin",                        
            "Transfer"                       
        );

        Block block = new Block(Collections.singletonList(fakeTx));

        block.setPreviousHash("0x" + UUID.randomUUID().toString().replace("-", ""));
        block.setTransactionLogs(List.of("⚠️ Transaction for byzantine node"));

        return block;
    }

    private static String randomId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
