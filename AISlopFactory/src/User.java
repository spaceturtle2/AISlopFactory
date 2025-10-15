import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared User class in the default package (no package declaration).
 * Keep serialVersionUID stable across apps so serialized files remain compatible.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 3L;

    private String username;
    private String password;
    private double balance;
    private double loanBalance;
    private Map<String, Integer> portfolio;

    public User() {
        this.username = "";
        this.password = "";
        this.balance = 0.0;
        this.loanBalance = 0.0;
        this.portfolio = new HashMap<>();
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance = 0.0;
        this.loanBalance = 0.0;
        this.portfolio = new HashMap<>();
    }

    // --- Getters / Setters ---
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

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

    // --- Banking helpers ---
    public synchronized void deposit(double amount) {
        if (amount > 0 && Double.isFinite(amount)) this.balance += amount;
    }
    public synchronized boolean withdraw(double amount) {
        if (amount > 0 && Double.isFinite(amount)) {
            this.balance -= amount; // allows debt
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
}
