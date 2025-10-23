import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * User model.
 * - serialVersionUID kept at 3L for compatibility with previously-saved files.
 * - Raw password is transient (not serialized). Stores passwordHash + passwordSalt.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 3L;

    private String username;
    private transient String password;       // transient: do not serialize raw password
    private String passwordHash;             // serialized
    private String passwordSalt;             // serialized (base64)

    private double balance;
    private double loanBalance;
    private Map<String, Integer> portfolio;

    public User() {
        this.username = "";
        this.balance = 0.0;
        this.loanBalance = 0.0;
        this.portfolio = new HashMap<>();
    }

    public User(String username, String rawPassword) {
        this();
        this.username = username;
        if (rawPassword != null) setPassword(rawPassword);
    }

    // --- Basic getters / setters ---
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    /** transient raw password (only used in-memory during login/register) */
    public String getPassword() { return password; }
    public void setPasswordTransient(String rawPassword) { this.password = rawPassword; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getLoanBalance() { return loanBalance; }
    public void setLoanBalance(double loanBalance) { this.loanBalance = loanBalance; }

    public Map<String, Integer> getPortfolio() {
        if (portfolio == null) portfolio = new HashMap<>();
        return portfolio;
    }
    public void setPortfolio(Map<String, Integer> portfolio) { this.portfolio = portfolio; }

    // --- Portfolio helpers ---
    public int getPortfolioQuantity(String ticker) { return getPortfolio().getOrDefault(ticker, 0); }
    public void addToPortfolio(String ticker, int qty) {
        if (qty <= 0) return;
        getPortfolio().put(ticker, getPortfolioQuantity(ticker) + qty);
    }
    public void removeFromPortfolio(String ticker, int qty) {
        if (qty <= 0) return;
        int have = getPortfolioQuantity(ticker);
        int remain = Math.max(0, have - qty);
        if (remain == 0) getPortfolio().remove(ticker);
        else getPortfolio().put(ticker, remain);
    }

    // --- Banking helpers (thread-safe) ---
    public synchronized void deposit(double amount) {
        if (amount > 0 && Double.isFinite(amount)) this.balance += amount;
    }
    public synchronized boolean withdraw(double amount) {
        if (amount > 0 && Double.isFinite(amount)) {
            this.balance -= amount; // allows negative balances
            return true;
        }
        return false;
    }

    public synchronized void addInterest(double rate) {
        if (this.balance > 0 && Double.isFinite(rate)) this.balance += this.balance * rate;
    }
    public synchronized void chargeDebtFee(double rate) {
        if (this.balance < 0 && Double.isFinite(rate)) {
            double fee = Math.abs(this.balance) * rate;
            this.balance -= fee;
        }
    }
    public synchronized void requestLoan(double amount) {
        if (amount > 0 && Double.isFinite(amount)) {
            this.loanBalance += amount;
            this.balance += amount;
        }
    }
    public synchronized double repayLoan(double amount) {
        if (amount <= 0) return 0.0;
        double toApply = Math.min(amount, this.loanBalance);
        this.loanBalance -= toApply;
        this.balance -= toApply;
        return toApply;
    }
    public synchronized void accrueLoanInterest(double rate) {
        if (this.loanBalance > 0 && Double.isFinite(rate)) this.loanBalance += this.loanBalance * rate;
    }

    // --- Password handling (SHA-256 + salt) ---
    /**
     * Sets the password: generates a random salt and stores salted hash.
     * (For production, prefer BCrypt/Argon2; SHA-256 here is simple.)
     */
    public void setPassword(String rawPassword) {
        if (rawPassword == null) return;
        byte[] saltBytes = generateSalt(16);
        this.passwordSalt = Base64.getEncoder().encodeToString(saltBytes);
        this.passwordHash = hashPassword(rawPassword, saltBytes);
        this.password = null; // forget raw password
    }

    /**
     * Verify a raw password against the stored hash+salt.
     */
    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null || passwordHash == null || passwordSalt == null) return false;
        byte[] saltBytes = Base64.getDecoder().decode(passwordSalt);
        String candidate = hashPassword(rawPassword, saltBytes);
        return passwordHash.equals(candidate);
    }

    // exposed for Save/Load utility (to write/read slots)
    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }

    // direct setters used by loader when restoring hashed credentials from snapshot
    public void setPasswordHashDirect(String hash) { this.passwordHash = hash; }
    public void setPasswordSaltDirect(String salt) { this.passwordSalt = salt; }

    // --- Helpers ---
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", balance=" + balance +
                ", loanBalance=" + loanBalance +
                ", portfolio=" + portfolio +
                '}';
    }

    private static byte[] generateSalt(int len) {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[len];
        sr.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String rawPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(rawPassword.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
