// StockSimulator.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import com.turtlestate.bank.model.User;

/**
 * Stock Investing Simulator that re-uses the banking_data.dat file and the same User structure.
 *
 * - Reads/writes Map<String, User> from/to banking_data.dat
 * - Simulated market with few tickers whose prices move every second (random walk)
 * - Users can log in with same usernames/passwords; buy/sell updates User.balance and User.portfolio
 * - User.portfolio is a Map<String,Integer> stored inside User; safe for older saved objects
 *
 * NOTE: Keep a backup of your banking_data.dat if you want to be safe while testing.
 */
public class StockSimulator extends JFrame {

    private static final String DATA_FILE = "banking_data.dat";
    private static final double MAX_TRADE_AMOUNT = 1_000_000_000.0;

    // Market tickers & starting prices
    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private final ScheduledExecutorService marketExecutor = Executors.newSingleThreadScheduledExecutor();

    // Accounts loaded from the banking_data file
    private static Map<String, User> accounts = new ConcurrentHashMap<>();

    // GUI
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // Auth
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel authStatus;

    // Trading UI
    private JLabel welcomeLabel;
    private JLabel cashLabel;
    private JLabel portfolioValueLabel;
    private JTable marketTable;
    private DefaultTableModel marketTableModel;
    private JTextField tradeTickerField;
    private JTextField tradeSharesField;
    private JButton buyButton;
    private JButton sellButton;
    private JTable portfolioTable;
    private DefaultTableModel portfolioTableModel;
    private JLabel unrealizedLabel;
    private JButton saveButton;
    private JButton logoutButton;

    private User loggedInUser = null;

    public StockSimulator() {
        super("Stock Simulator (uses banking_data.dat)");
        loadData(); // loads 'accounts'
        initMarket();
        buildUI();
        startMarket();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
    }

    // --- UI Construction ---

    private void buildUI() {
        // Add panels into the CardLayout container (mainPanel) — NOT directly to the frame
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createTradingPanel(), "TRADING");
        cardLayout.show(mainPanel, "LOGIN");

        // Add the mainPanel to the frame once
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new BorderLayout(12,12));
        p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JLabel title = new JLabel("Stock Simulator — Login (uses banking_data.dat)", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        p.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.gridx=0; c.gridy=0; c.anchor = GridBagConstraints.EAST;
        center.add(new JLabel("Username:"), c);
        c.gridx=1; c.anchor = GridBagConstraints.WEST;
        usernameField = new JTextField(14);
        center.add(usernameField, c);

        c.gridx=0; c.gridy=1; c.anchor = GridBagConstraints.EAST;
        center.add(new JLabel("Password:"), c);
        c.gridx=1; c.anchor = GridBagConstraints.WEST;
        passwordField = new JPasswordField(14);
        center.add(passwordField, c);

        c.gridx=0; c.gridy=2; c.gridwidth=2;
        authStatus = new JLabel("Enter credentials (same as BankingApp)", SwingConstants.CENTER);
        center.add(authStatus, c);

        p.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton loginBtn = new JButton("Log In");
        loginBtn.addActionListener(e -> login());
        JButton createBtn = new JButton("Create Account");
        createBtn.addActionListener(e -> createAccount());
        bottom.add(loginBtn);
        bottom.add(createBtn);

        p.add(bottom, BorderLayout.SOUTH);

        mainPanel.add(p, "LOGIN");
        return p;
    }

    private JPanel createTradingPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Top: Welcome + cash + portfolio value
        JPanel top = new JPanel(new GridLayout(2,2,8,8));
        welcomeLabel = new JLabel("Not logged in");
        cashLabel = new JLabel("Cash: $0.00");
        portfolioValueLabel = new JLabel("Portfolio Value: $0.00");
        unrealizedLabel = new JLabel("Unrealized P/L: $0.00");
        top.add(welcomeLabel);
        top.add(cashLabel);
        top.add(portfolioValueLabel);
        top.add(unrealizedLabel);
        p.add(top, BorderLayout.NORTH);

        // Center split: market left, portfolio right
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.6);

        // Market panel
        JPanel marketPanel = new JPanel(new BorderLayout(6,6));
        marketPanel.setBorder(BorderFactory.createTitledBorder("Market"));
        marketTableModel = new DefaultTableModel(new Object[]{"Ticker","Price","Change"},0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        marketTable = new JTable(marketTableModel);
        marketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane marketScroll = new JScrollPane(marketTable);
        marketPanel.add(marketScroll, BorderLayout.CENTER);

        JPanel tradePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8,8));
        tradeTickerField = new JTextField(6);
        tradeSharesField = new JTextField(6);
        buyButton = new JButton("Buy");
        sellButton = new JButton("Sell");
        buyButton.addActionListener(e -> executeBuy());
        sellButton.addActionListener(e -> executeSell());
        tradePanel.add(new JLabel("Ticker:"));
        tradePanel.add(tradeTickerField);
        tradePanel.add(new JLabel("Shares:"));
        tradePanel.add(tradeSharesField);
        tradePanel.add(buyButton);
        tradePanel.add(sellButton);
        marketPanel.add(tradePanel, BorderLayout.SOUTH);

        split.setLeftComponent(marketPanel);

        // Portfolio panel
        JPanel portPanel = new JPanel(new BorderLayout(6,6));
        portPanel.setBorder(BorderFactory.createTitledBorder("Portfolio"));
        portfolioTableModel = new DefaultTableModel(new Object[]{"Ticker","Qty","Price","Value"}, 0) {
            public boolean isCellEditable(int r,int c){return false;}
        };
        portfolioTable = new JTable(portfolioTableModel);
        JScrollPane portScroll = new JScrollPane(portfolioTable);
        portPanel.add(portScroll, BorderLayout.CENTER);

        JPanel portBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8,8));
        saveButton = new JButton("Save (persist)");
        saveButton.addActionListener(e -> { saveData(); authStatus.setText("Data saved."); });
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        portBottom.add(saveButton);
        portBottom.add(logoutButton);
        portPanel.add(portBottom, BorderLayout.SOUTH);

        split.setRightComponent(portPanel);

        p.add(split, BorderLayout.CENTER);

        // Bottom: market controls (simulate single-step price jump)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8,8));
        JButton stepButton = new JButton("Step Market (+ random)");
        stepButton.addActionListener(e -> randomWalkOnce());
        bottom.add(stepButton);

        JLabel hint = new JLabel("Prices auto-update once per second. Trades modify your bank cash and persist to banking_data.dat");
        bottom.add(hint);

        p.add(bottom, BorderLayout.SOUTH);

        mainPanel.add(p, "TRADING");
        return p;
    }

    // --- Auth & Account management ---

    private void login() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            authStatus.setText("Enter username and password.");
            authStatus.setForeground(Color.RED);
            return;
        }

        User u = accounts.get(user);
        if (u != null && u.getPassword().equals(pass)) {
            loggedInUser = u;
            if (loggedInUser.getPortfolio() == null) loggedInUser.initPortfolio();
            SwingUtilities.invokeLater(() -> {
                updateTradingUI();
                cardLayout.show(mainPanel, "TRADING");
                authStatus.setText("Logged in.");
                authStatus.setForeground(Color.BLUE);
            });
        } else {
            authStatus.setText("Invalid user/password.");
            authStatus.setForeground(Color.RED);
        }
    }

    private void createAccount() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            authStatus.setText("Username/password cannot be empty.");
            authStatus.setForeground(Color.RED);
            return;
        }
        if (accounts.containsKey(user)) {
            authStatus.setText("Username exists.");
            authStatus.setForeground(Color.RED);
            return;
        }
        User u = new User(user, pass);
        u.initPortfolio();
        accounts.put(user, u);
        saveData();
        loggedInUser = u;
        SwingUtilities.invokeLater(() -> {
            updateTradingUI();
            cardLayout.show(mainPanel, "TRADING");
            authStatus.setText("Account created & logged in.");
            authStatus.setForeground(Color.BLUE);
        });
    }

    private void logout() {
        loggedInUser = null;
        usernameField.setText("");
        passwordField.setText("");
        cardLayout.show(mainPanel, "LOGIN");
    }

    // --- Market simulation ---

    private void initMarket() {
        // starting prices
        prices.put("AAPL", 175.00);
        prices.put("GOOG", 2900.00);
        prices.put("AMZN", 135.00);
        prices.put("MSFT", 330.00);
        prices.put("TSLA", 250.00);
        prices.keySet().forEach(k -> lastPrices.put(k, prices.get(k)));
    }

    private void startMarket() {
        Runnable marketTask = () -> {
            randomWalkOnce();
            SwingUtilities.invokeLater(this::refreshMarketTable);
        };
        marketExecutor.scheduleAtFixedRate(marketTask, 1, 1, TimeUnit.SECONDS);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                marketExecutor.shutdownNow();
                saveData();
            }
        });
    }

    private void randomWalkOnce() {
        Random rnd = new Random();
        for (String t : prices.keySet()) {
            double p = prices.get(t);
            lastPrices.put(t, p);
            // small percent move, biased random walk
            double pct = (rnd.nextGaussian() * 0.01); // ~1% stdev
            // clamp
            pct = Math.max(-0.12, Math.min(0.12, pct));
            double np = Math.max(0.01, p * (1.0 + pct));
            prices.put(t, Math.round(np * 100.0) / 100.0);
        }
        SwingUtilities.invokeLater(() -> {
            refreshMarketTable();
            if (loggedInUser != null) updateTradingUI();
        });
    }

    private void refreshMarketTable() {
        SwingUtilities.invokeLater(() -> {
            marketTableModel.setRowCount(0);
            for (String t : new ArrayList<>(prices.keySet())) {
                double p = prices.get(t);
                double lp = lastPrices.getOrDefault(t, p);
                String ch = String.format("%+.2f", p - lp);
                marketTableModel.addRow(new Object[]{t, String.format("$%.2f", p), ch});
            }
        });
    }

    // --- Trading logic ---

    private void executeBuy() {
        if (loggedInUser == null) return;
        String ticker = tradeTickerField.getText().trim().toUpperCase();
        if (!prices.containsKey(ticker)) {
            showError("Ticker not in market: " + ticker);
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(tradeSharesField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter integer share quantity.");
            return;
        }
        if (qty <= 0) { showError("Quantity must be positive."); return; }
        double price = prices.get(ticker);
        double cost = price * qty;
        if (!Double.isFinite(cost) || cost > MAX_TRADE_AMOUNT) { showError("Invalid trade amount."); return; }

        synchronized (loggedInUser) {
            if (loggedInUser.getBalance() < cost) {
                showError("Insufficient cash to buy. Needed: $" + String.format("%.2f", cost));
                return;
            }
            // deduct cash and add to portfolio
            loggedInUser.withdraw(cost); // uses existing withdraw which allows negative but we check
            loggedInUser.addToPortfolio(ticker, qty);
            saveData();
        }
        updateTradingUI();
        tradeTickerField.setText("");
        tradeSharesField.setText("");
    }

    private void executeSell() {
        if (loggedInUser == null) return;
        String ticker = tradeTickerField.getText().trim().toUpperCase();
        if (!prices.containsKey(ticker)) {
            showError("Ticker not in market: " + ticker);
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(tradeSharesField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter integer share quantity.");
            return;
        }
        if (qty <= 0) { showError("Quantity must be positive."); return; }

        synchronized (loggedInUser) {
            int have = loggedInUser.getPortfolioQuantity(ticker);
            if (have < qty) {
                showError("Not enough shares. You have " + have);
                return;
            }
            double price = prices.get(ticker);
            double proceeds = price * qty;
            loggedInUser.removeFromPortfolio(ticker, qty);
            loggedInUser.deposit(proceeds);
            saveData();
        }
        updateTradingUI();
        tradeTickerField.setText("");
        tradeSharesField.setText("");
    }

    // --- UI updates ---

    private void updateTradingUI() {
        if (loggedInUser == null) return;
        welcomeLabel.setText("User: " + loggedInUser.getUsername());
        cashLabel.setText(String.format("Cash: $%.2f", loggedInUser.getBalance()));

        // update portfolio table & compute value/PL
        Map<String,Integer> portfolio = loggedInUser.getPortfolio();
        portfolioTableModel.setRowCount(0);
        double totalValue = 0.0;
        double costBasis = 0.0; // we don't track cost basis; use zero — unrealized = value
        for (String t : new TreeSet<>(portfolio.keySet())) {
            int q = portfolio.getOrDefault(t, 0);
            if (q == 0) continue;
            double p = prices.getOrDefault(t, 0.0);
            double val = p * q;
            totalValue += val;
            portfolioTableModel.addRow(new Object[]{t, q, String.format("$%.2f", p), String.format("$%.2f", val)});
        }
        portfolioValueLabel.setText(String.format("Portfolio Value: $%.2f", totalValue));
        // unrealized P/L approximate (we don't track basis), show just portfolio value
        unrealizedLabel.setText(String.format("Unrealized (approx): $%.2f", totalValue));
    }

    private void showError(String msg) {
        bankingNotify(msg);
        JOptionPane.showMessageDialog(this, msg, "Trade Error", JOptionPane.ERROR_MESSAGE);
    }

    private void bankingNotify(String msg) {
        authStatus.setText(msg);
        authStatus.setForeground(Color.RED);
    }

    // --- Persistence ---

    @SuppressWarnings("unchecked")
    private void loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) {
            accounts = new ConcurrentHashMap<>();
            System.out.println("No data file found; starting with empty accounts.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(f);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            if (!(obj instanceof Map)) {
                System.err.println("Unexpected data in " + DATA_FILE);
                accounts = new ConcurrentHashMap<>();
                return;
            }

            Map<?,?> raw = (Map<?,?>) obj;
            Map<String, User> rebuilt = new ConcurrentHashMap<>();

            for (Map.Entry<?,?> e : raw.entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                if (!(k instanceof String)) continue;
                String username = (String) k;

                if (v == null) {
                    // create a blank user to be safe
                    User u = new User(username, ""); u.setBalance(0.0); u.setLoanBalance(0.0); u.initPortfolio();
                    rebuilt.put(username, u);
                    continue;
                }

                if (v instanceof User) {
                    // already our simulator's User (nice)
                    User existing = (User) v;
                    // ensure portfolio non-null
                    if (existing.getPortfolio() == null) existing.initPortfolio();
                    rebuilt.put(username, existing);
                    continue;
                }

                // Otherwise: the object came from another class (e.g. BankingApp$User).
                // Use reflection to extract expected properties and create a new User.
                String uname = null;
                String pass = "";
                double balance = 0.0;
                double loan = 0.0;
                Map<String,Integer> portfolio = null;

                Class<?> cls = v.getClass();
                try {
                    // Try getters first (common)
                    try {
                        java.lang.reflect.Method m = cls.getMethod("getUsername");
                        Object r = m.invoke(v);
                        if (r != null) uname = r.toString();
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        java.lang.reflect.Method m = cls.getMethod("getPassword");
                        Object r = m.invoke(v);
                        if (r != null) pass = r.toString();
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        java.lang.reflect.Method m = cls.getMethod("getBalance");
                        Object r = m.invoke(v);
                        if (r instanceof Number) balance = ((Number) r).doubleValue();
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        java.lang.reflect.Method m = cls.getMethod("getLoanBalance");
                        Object r = m.invoke(v);
                        if (r instanceof Number) loan = ((Number) r).doubleValue();
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        java.lang.reflect.Method m = cls.getMethod("getPortfolio");
                        Object r = m.invoke(v);
                        if (r instanceof Map) {
                            // try to coerce to Map<String,Integer>
                            portfolio = new HashMap<>();
                            for (Map.Entry<?,?> pe : ((Map<?,?>) r).entrySet()) {
                                Object pk = pe.getKey();
                                Object pv = pe.getValue();
                                if (pk != null && pv instanceof Number) {
                                    portfolio.put(pk.toString(), ((Number) pv).intValue());
                                }
                            }
                        }
                    } catch (NoSuchMethodException ignored) {}
                } catch (Exception reflectionEx) {
                    // ignore and try direct field access next
                }

                // If getters failed to yield username/password, attempt to read fields directly
                if (uname == null) {
                    try {
                        java.lang.reflect.Field fUser = cls.getDeclaredField("username");
                        fUser.setAccessible(true);
                        Object ru = fUser.get(v);
                        if (ru != null) uname = ru.toString();
                    } catch (Exception ignored) {}
                }
                if ("".equals(pass)) {
                    try {
                        java.lang.reflect.Field fPass = cls.getDeclaredField("password");
                        fPass.setAccessible(true);
                        Object rp = fPass.get(v);
                        if (rp != null) pass = rp.toString();
                    } catch (Exception ignored) {}
                }
                try {
                    java.lang.reflect.Field fBal = null;
                    try { fBal = cls.getDeclaredField("balance"); } catch (NoSuchFieldException ignored) {}
                    if (fBal != null) {
                        fBal.setAccessible(true);
                        Object rb = fBal.get(v);
                        if (rb instanceof Number) balance = ((Number) rb).doubleValue();
                    }
                } catch (Exception ignored) {}
                try {
                    java.lang.reflect.Field fLoan = null;
                    try { fLoan = cls.getDeclaredField("loanBalance"); } catch (NoSuchFieldException ignored) {}
                    if (fLoan != null) {
                        fLoan.setAccessible(true);
                        Object rl = fLoan.get(v);
                        if (rl instanceof Number) loan = ((Number) rl).doubleValue();
                    }
                } catch (Exception ignored) {}
                try {
                    java.lang.reflect.Field fPort = null;
                    try { fPort = cls.getDeclaredField("portfolio"); } catch (NoSuchFieldException ignored) {}
                    if (fPort != null) {
                        fPort.setAccessible(true);
                        Object rp = fPort.get(v);
                        if (rp instanceof Map) {
                            portfolio = new HashMap<>();
                            for (Map.Entry<?,?> pe : ((Map<?,?>) rp).entrySet()) {
                                Object pk = pe.getKey();
                                Object pv = pe.getValue();
                                if (pk != null && pv instanceof Number) {
                                    portfolio.put(pk.toString(), ((Number) pv).intValue());
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // Fallbacks
                if (uname == null) uname = username; // use map key
                // create new User in simulator format
                User nu = new User(uname, pass == null ? "" : pass);
                nu.setBalance(balance);
                nu.setLoanBalance(loan);
                nu.initPortfolio();
                if (portfolio != null) {
                    for (Map.Entry<String,Integer> pe : portfolio.entrySet()) {
                        if (pe.getKey() != null && pe.getValue() != null) {
                            nu.addToPortfolio(pe.getKey(), pe.getValue());
                        }
                    }
                }
                rebuilt.put(uname, nu);
            }

            accounts = new ConcurrentHashMap<>(rebuilt);
            System.out.println("Loaded and converted accounts from " + DATA_FILE);
        } catch (Exception ex) {
            ex.printStackTrace();
            accounts = new ConcurrentHashMap<>();
        }
    }

    private synchronized void saveData() {
        try (FileOutputStream fos = new FileOutputStream(DATA_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(accounts);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // --- Shutdown helpers ---

    private void shutdown() {
        marketExecutor.shutdownNow();
        saveData();
    }

    // --- Main ---

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StockSimulator s = new StockSimulator();
            s.setVisible(true);
        });
    }

    // --- User class compatible with BankingApp's User ---
    // serialVersionUID kept as 3L to match the BankingApp code you used previously.
   }
