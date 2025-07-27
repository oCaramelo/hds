package tecnico.models;

import java.math.BigInteger;
import tecnico.logger.CustomLogger;

public class Account {
    private static final CustomLogger logger = new CustomLogger(Account.class.getName());
    private final String address;
    private BigInteger depCoinBalance;
    private BigInteger istCoinBalance;

    public Account(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public BigInteger getDepCoinBalance() {
        return depCoinBalance;
    }

    public void setDepCoinBalance(BigInteger balance) {
        // logger.info("[DEBUG] Setting DepCoin balance for " + address + " to " + balance);
        this.depCoinBalance = balance;
    }

    public BigInteger getIstCoinBalance() {
        return istCoinBalance;
    }

    public void setIstCoinBalance(BigInteger istCoinBalance) {
        this.istCoinBalance = istCoinBalance;
    }

    public void addDepCoin(BigInteger amount) {
        // logger.info("[DEBUG] Adding " + amount + " DepCoin to " + address);
        this.depCoinBalance = this.depCoinBalance.add(amount);
    }

    public void subtractDepCoin(BigInteger amount) {
        // logger.info("[DEBUG] Subtracting " + amount + " DepCoin from " + address);
        this.depCoinBalance = this.depCoinBalance.subtract(amount);
    }


}