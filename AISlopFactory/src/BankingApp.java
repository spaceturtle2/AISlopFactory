// BankingApp.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * BankingApp with compact UI + stats panel + pie chart.
 * - Keeps all previous behaviors: interest, debt fee, loans, admin controls, fullscreen.
 * - Shrinked text fields and rearranged controls so UI doesn't feel blown out.
 * - StatsPanel draws a pie chart and textual stats and repaints on data/ UI updates.
 */
public class BankingApp extends JFrame {

    // --- Configuration Constants ---
    private static final double MAX_TRANSACTION_LIMIT = 1_000_000_000.0; // Max deposit/withdraw amount (1 Billion)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "1234";

    // DEBT CONSTANT: 0.5% fee charged on negative balances every second
    private static final double DEBT_FEE_RATE = 0.005;

    // Default loan settings (can be changed by admin at runtime)
    private double globalLoanLimit = 5_000.0;
    private double globalLoanInterestRate = 0.002; // 0.2% per second (accrued on loanBalance)

    // --- Data Storage and Services ---
    private static Map<String, User> accounts = new ConcurrentHashMap<>();
    private User loggedInUser = null;
    private static final String DATA_FILE = "banking_data.dat";
    private final ScheduledExecutorService interestScheduler = Executors.newSingleThreadScheduledExecutor();

    // --- GUI Components ---
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // Auth Components
    private JTextField authUsernameField;
    private JPasswordField authPasswordField;
    private JLabel authStatusLabel;

    // Banking/Transaction Components (Used by regular users)
    private JLabel welcomeLabel;
    private JLabel balanceLabel;
    private JLabel loanLabel; // shows loan balance for user
    private JTextField transactionField; // For Deposit/Withdraw
    private JButton depositButton;
    private JButton withdrawButton;
    private JPanel centerViewContainer; // Container to switch between User/Admin views

    // User Transfer Components
    private JTextField userTransferRecipientField; // For user-to-user transfer recipient
    private JTextField userTransferAmountField;    // For user-to-user transfer amount
    private JButton userTransferButton;

    // Loan Components (User)
    private JTextField loanRequestField;
    private JButton requestLoanButton;
    private JTextField loanRepayField;
    private JButton repayLoanButton;

    // Admin Components (Used by admin user)
    private JList<String> userList; // Displays list of users and balances
    private DefaultListModel<String> listModel; // Model for the user list
    private JButton resetButton; // Reset Selected Balance
    private JPanel adminViewPanel; // The admin-specific view panel

    // Admin Transfer Components (Used by admin user)
    private JTextField recipientUsernameField; // For admin grant recipient
    private JTextField transferAmountField;    // For admin grant amount
    private JButton transferButton;

    // Admin loan control components
    private JTextField adminLoanLimitField;
    private JTextField adminLoanRateField;
    private JButton applyLoanSettingsButton;
    private JButton forgiveLoanButton; // Forgive selected user's loan

    // Shared Status Label
    private JLabel bankingStatusLabel;

    // Stats panel (small, right-side)
    private StatsPanel statsPanel;

    public BankingApp() {
        super("Gemini Bank - Secure Console");

        // 1. Load Data and Start Services
        loadData();
        startInterestService();

        // 2. Setup Main Frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Make frame undecorated and full-screen before showing
        setUndecorated(true);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setResizable(false);
        setLocationRelativeTo(null);

        // 3. Create Panels
        JPanel authPanel = createAuthPanel();
        JPanel bankingPanel = createBankingPanel();

        // 4. Add Panels to CardLayout
        mainPanel.add(authPanel, "AUTH");
        mainPanel.add(bankingPanel, "BANKING");

        add(mainPanel);

        // Show the initial view
        cardLayout.show(mainPanel, "AUTH");
    }

    // --- Panel Creation ---

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 248, 255)); // Alice Blue

        JLabel title = new JLabel("Welcome to Gemini Bank", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(new Color(60, 179, 113));
        panel.add(title, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(panel.getBackground());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Username:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        authUsernameField = new JTextField(12); // smaller
        inputPanel.add(authUsernameField, c);

        c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Password:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        authPasswordField = new JPasswordField(12); // smaller
        inputPanel.add(authPasswordField, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        authStatusLabel = new JLabel("Please log in or create an account.", SwingConstants.CENTER);
        authStatusLabel.setForeground(Color.BLUE);
        inputPanel.add(authStatusLabel, c);

        panel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        buttonPanel.setBackground(panel.getBackground());

        JButton loginButton = new JButton("Log In");
        loginButton.addActionListener(e -> logIn());

        JButton createButton = new JButton("Create Account");
        createButton.addActionListener(e -> createAccount());

        loginButton.setBackground(new Color(144, 238, 144));
        createButton.setBackground(new Color(173, 216, 230));

        buttonPanel.add(loginButton);
        buttonPanel.add(createButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBankingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(255, 255, 224)); // Light Yellow

        // Top Info Panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        infoPanel.setBackground(panel.getBackground());

        welcomeLabel = new JLabel("", SwingConstants.LEFT);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        welcomeLabel.setForeground(new Color(0, 100, 0));

        balanceLabel = new JLabel("Balance: $0.00", SwingConstants.LEFT);
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 20));
        balanceLabel.setForeground(Color.RED);

        loanLabel = new JLabel("Loan: $0.00", SwingConstants.LEFT);
        loanLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loanLabel.setForeground(new Color(139, 0, 0));

        infoPanel.add(welcomeLabel);
        infoPanel.add(balanceLabel);
        infoPanel.add(loanLabel);

        panel.add(infoPanel, BorderLayout.NORTH);

        // Center left - main controls; Right - statistics (compact)
        JPanel centerSplit = new JPanel(new BorderLayout(8, 8));
        centerViewContainer = new JPanel(new CardLayout());

        // User transaction view and Admin view
        centerViewContainer.add(createUserTransactionPanel(), "USER_VIEW");
        adminViewPanel = createAdminViewPanel();
        centerViewContainer.add(adminViewPanel, "ADMIN_VIEW");

        centerSplit.add(centerViewContainer, BorderLayout.CENTER);

        statsPanel = new StatsPanel();
        statsPanel.setPreferredSize(new Dimension(320, 240)); // small panel
        centerSplit.add(statsPanel, BorderLayout.EAST);

        panel.add(centerSplit, BorderLayout.CENTER);

        // Status + logout
        bankingStatusLabel = new JLabel("Ready for transaction.", SwingConstants.LEFT);
        bankingStatusLabel.setForeground(Color.BLACK);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        JPanel south = new JPanel(new BorderLayout(6, 6));
        south.setBackground(panel.getBackground());
        south.add(bankingStatusLabel, BorderLayout.CENTER);
        south.add(logoutButton, BorderLayout.EAST);

        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    /** Creates the panel for regular users (deposit/withdraw/transfer + loan) */
    private JPanel createUserTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Transaction Area (Debt & Loans Allowed)"));
        panel.setBackground(new Color(255, 255, 224));

        // Compact deposit/withdraw area
        JPanel dwPanel = new JPanel(new GridBagLayout());
        dwPanel.setBackground(panel.getBackground());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
        dwPanel.add(new JLabel("Amount:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        transactionField = new JTextField(8); // smaller
        dwPanel.add(transactionField, c);

        c.gridx = 2;
        depositButton = new JButton("Deposit");
        depositButton.addActionListener(e -> deposit());
        depositButton.setBackground(new Color(152, 251, 152));
        dwPanel.add(depositButton, c);

        c.gridx = 3;
        withdrawButton = new JButton("Withdraw");
        withdrawButton.addActionListener(e -> withdraw());
        withdrawButton.setBackground(new Color(255, 160, 122));
        dwPanel.add(withdrawButton, c);

        panel.add(dwPanel, BorderLayout.NORTH);

        // Center: transfer and loan compact panels side by side
        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setBackground(panel.getBackground());

        // Transfer small panel
        JPanel transferPanel = new JPanel(new GridBagLayout());
        transferPanel.setBorder(BorderFactory.createTitledBorder("Send Transfer"));
        transferPanel.setBackground(panel.getBackground());
        GridBagConstraints t = new GridBagConstraints();
        t.insets = new Insets(4, 4, 4, 4);
        t.gridx = 0; t.gridy = 0; t.anchor = GridBagConstraints.EAST;
        transferPanel.add(new JLabel("To:"), t);
        t.gridx = 1; t.anchor = GridBagConstraints.WEST;
        userTransferRecipientField = new JTextField(8);
        transferPanel.add(userTransferRecipientField, t);

        t.gridx = 0; t.gridy = 1; t.anchor = GridBagConstraints.EAST;
        transferPanel.add(new JLabel("Amt:"), t);
        t.gridx = 1; t.anchor = GridBagConstraints.WEST;
        userTransferAmountField = new JTextField(8);
        transferPanel.add(userTransferAmountField, t);

        t.gridx = 0; t.gridy = 2; t.gridwidth = 2;
        t.anchor = GridBagConstraints.CENTER;
        userTransferButton = new JButton("Send");
        userTransferButton.addActionListener(e -> userTransfer());
        userTransferButton.setBackground(new Color(100, 149, 237));
        transferPanel.add(userTransferButton, t);

        center.add(transferPanel);

        // Loan small panel
        JPanel loanPanel = new JPanel(new GridBagLayout());
        loanPanel.setBorder(BorderFactory.createTitledBorder("Loan Services"));
        loanPanel.setBackground(panel.getBackground());
        GridBagConstraints l = new GridBagConstraints();
        l.insets = new Insets(4, 4, 4, 4);
        l.gridx = 0; l.gridy = 0; l.anchor = GridBagConstraints.EAST;
        loanPanel.add(new JLabel("Borrow:"), l);
        l.gridx = 1; l.anchor = GridBagConstraints.WEST;
        loanRequestField = new JTextField(6);
        loanPanel.add(loanRequestField, l);

        l.gridx = 2;
        requestLoanButton = new JButton("Request");
        requestLoanButton.addActionListener(e -> requestLoan());
        requestLoanButton.setBackground(new Color(255, 215, 0));
        loanPanel.add(requestLoanButton, l);

        l.gridx = 0; l.gridy = 1; l.anchor = GridBagConstraints.EAST;
        loanPanel.add(new JLabel("Repay:"), l);
        l.gridx = 1; l.anchor = GridBagConstraints.WEST;
        loanRepayField = new JTextField(6);
        loanPanel.add(loanRepayField, l);

        l.gridx = 2;
        repayLoanButton = new JButton("Repay");
        repayLoanButton.addActionListener(e -> repayLoan());
        repayLoanButton.setBackground(new Color(60, 179, 113));
        loanPanel.add(repayLoanButton, l);

        center.add(loanPanel);

        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    /** Creates the panel for admin to view users, reset balances, transfer funds, and manage loans */
    private JPanel createAdminViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Admin Control"));
        panel.setBackground(new Color(230, 230, 250)); // Lavender

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(userList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(3, 1, 6, 6));
        controls.setBackground(panel.getBackground());

        JPanel grant = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        grant.setBackground(panel.getBackground());
        grant.add(new JLabel("Recipient:"));
        recipientUsernameField = new JTextField(8);
        grant.add(recipientUsernameField);
        grant.add(new JLabel("Amt:"));
        transferAmountField = new JTextField(8);
        grant.add(transferAmountField);
        transferButton = new JButton("Grant");
        transferButton.addActionListener(e -> adminGrantTransfer());
        transferButton.setBackground(new Color(30, 144, 255));
        grant.add(transferButton);
        controls.add(grant);

        JPanel loanSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        loanSettings.setBackground(panel.getBackground());
        loanSettings.add(new JLabel("Loan Limit:"));
        adminLoanLimitField = new JTextField(String.format("%.2f", globalLoanLimit), 8);
        loanSettings.add(adminLoanLimitField);
        loanSettings.add(new JLabel("Rate/sec:"));
        adminLoanRateField = new JTextField(String.format("%.6f", globalLoanInterestRate), 8);
        loanSettings.add(adminLoanRateField);
        applyLoanSettingsButton = new JButton("Apply");
        applyLoanSettingsButton.addActionListener(e -> applyLoanSettings());
        loanSettings.add(applyLoanSettingsButton);
        controls.add(loanSettings);

        JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        adminActions.setBackground(panel.getBackground());
        resetButton = new JButton("Reset Balance");
        resetButton.addActionListener(e -> resetSelectedBalance());
        resetButton.setBackground(new Color(205, 92, 92));
        adminActions.add(resetButton);

        forgiveLoanButton = new JButton("Forgive Loan");
        forgiveLoanButton.addActionListener(e -> forgiveSelectedUserLoan());
        forgiveLoanButton.setBackground(new Color(218, 112, 214));
        adminActions.add(forgiveLoanButton);

        controls.add(adminActions);

        panel.add(controls, BorderLayout.SOUTH);

        return panel;
    }

    // --- Authentication Logic ---

    private void logIn() {
        String username = authUsernameField.getText();
        String password = new String(authPasswordField.getPassword());

        // --- ADMIN LOGIN CHECK ---
        if (username.equals(ADMIN_USER) && password.equals(ADMIN_PASS)) {
            loggedInUser = new User(ADMIN_USER, ADMIN_PASS);
            updateUserList();
            toggleAdminFeatures(true);
            cardLayout.show(mainPanel, "BANKING");
            authStatusLabel.setText("Admin Login successful! View and manage accounts.");
            authStatusLabel.setForeground(Color.BLUE);
            authPasswordField.setText("");
            return;
        }

        User user = accounts.get(username);

        if (user != null && user.getPassword().equals(password)) {
            loggedInUser = user;
            updateBankingUI();
            toggleAdminFeatures(false);
            cardLayout.show(mainPanel, "BANKING");
            authStatusLabel.setText("Login successful!");
            authStatusLabel.setForeground(Color.BLUE);
        } else {
            authStatusLabel.setText("Login failed: Invalid username or password.");
            authStatusLabel.setForeground(Color.RED);
        }
        authPasswordField.setText("");
    }

    private void createAccount() {
        String username = authUsernameField.getText().trim();
        String password = new String(authPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            authStatusLabel.setText("Username and password cannot be empty.");
            authStatusLabel.setForeground(Color.RED);
            return;
        }

        if (username.equals(ADMIN_USER)) {
            authStatusLabel.setText("Username 'admin' is reserved.");
            authStatusLabel.setForeground(Color.RED);
            return;
        }

        if (accounts.containsKey(username)) {
            authStatusLabel.setText("Account creation failed: Username already exists.");
            authStatusLabel.setForeground(Color.RED);
            return;
        }

        User newUser = new User(username, password);
        accounts.put(username, newUser);
        loggedInUser = newUser;
        saveData();

        updateBankingUI();
        toggleAdminFeatures(false);
        cardLayout.show(mainPanel, "BANKING");
        authStatusLabel.setText("Account created and logged in!");
        authStatusLabel.setForeground(Color.BLUE);

        authUsernameField.setText("");
        authPasswordField.setText("");
        // update stats
        statsPanel.repaint();
    }

    // --- Banking Logic ---

    private void deposit() {
        if (loggedInUser == null || loggedInUser.getUsername().equals(ADMIN_USER)) return;

        try {
            double amount = Double.parseDouble(transactionField.getText());

            if (!Double.isFinite(amount) || amount > MAX_TRANSACTION_LIMIT) {
                bankingStatusLabel.setText(String.format("Invalid or excessive amount. Max is $%.0f.", MAX_TRANSACTION_LIMIT));
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (amount > 0) {
                loggedInUser.deposit(amount);
                updateBankingUI();
                bankingStatusLabel.setText(String.format("$%.2f deposited successfully.", amount));
                bankingStatusLabel.setForeground(new Color(0, 128, 0)); // Green
                saveData();
            } else {
                bankingStatusLabel.setText("Deposit amount must be positive.");
                bankingStatusLabel.setForeground(Color.RED);
            }
        } catch (NumberFormatException e) {
            bankingStatusLabel.setText("Invalid amount entered. Please use numbers.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            transactionField.setText("");
            statsPanel.repaint();
        }
    }

    private void withdraw() {
        if (loggedInUser == null || loggedInUser.getUsername().equals(ADMIN_USER)) return;

        try {
            double amount = Double.parseDouble(transactionField.getText());

            if (!Double.isFinite(amount) || amount > MAX_TRANSACTION_LIMIT) {
                bankingStatusLabel.setText(String.format("Invalid or excessive amount. Max is $%.0f.", MAX_TRANSACTION_LIMIT));
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (amount > 0) {
                loggedInUser.withdraw(amount);
                updateBankingUI();
                saveData();

                if (loggedInUser.getBalance() < 0) {
                    bankingStatusLabel.setText(String.format("WITHDRAWAL SUCCESS: $%.2f. You are now in DEBT!", amount));
                    bankingStatusLabel.setForeground(new Color(255, 140, 0)); // Dark Orange alert
                } else {
                    bankingStatusLabel.setText(String.format("$%.2f withdrawn successfully.", amount));
                    bankingStatusLabel.setForeground(new Color(0, 128, 0)); // Green
                }
            } else {
                bankingStatusLabel.setText("Withdrawal amount must be positive.");
                bankingStatusLabel.setForeground(Color.RED);
            }
        } catch (NumberFormatException e) {
            bankingStatusLabel.setText("Invalid amount entered. Please use numbers.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            transactionField.setText("");
            statsPanel.repaint();
        }
    }

    /** User-to-User fund transfer */
    private void userTransfer() {
        if (loggedInUser == null || loggedInUser.getUsername().equals(ADMIN_USER)) return;

        String recipient = userTransferRecipientField.getText().trim();
        String amountText = userTransferAmountField.getText().trim();
        double amount;

        if (recipient.isEmpty()) {
            bankingStatusLabel.setText("Recipient username cannot be empty for transfer.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        if (recipient.equals(loggedInUser.getUsername())) {
            bankingStatusLabel.setText("Cannot transfer funds to your own account.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        User recipientUser = accounts.get(recipient);
        if (recipientUser == null) {
            bankingStatusLabel.setText("Error: Recipient user '" + recipient + "' does not exist.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        try {
            amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                bankingStatusLabel.setText("Transfer amount must be positive.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (!Double.isFinite(amount) || amount > MAX_TRANSACTION_LIMIT) {
                bankingStatusLabel.setText(String.format("Invalid or excessive amount. Max is $%.0f.", MAX_TRANSACTION_LIMIT));
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            loggedInUser.withdraw(amount);
            recipientUser.deposit(amount);

            saveData();
            updateBankingUI();

            if (loggedInUser.getBalance() < 0) {
                bankingStatusLabel.setText(String.format("TRANSFER SUCCESS: $%.2f sent to '%s'. Sender is now in DEBT!", amount, recipient));
                bankingStatusLabel.setForeground(new Color(255, 140, 0)); // Dark Orange alert
            } else {
                bankingStatusLabel.setText(String.format("SUCCESS: Transferred $%.2f to '%s'.", amount, recipient));
                bankingStatusLabel.setForeground(new Color(0, 128, 0)); // Green
            }

        } catch (NumberFormatException e) {
            bankingStatusLabel.setText("Invalid amount entered. Please use numbers for the amount.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            userTransferRecipientField.setText("");
            userTransferAmountField.setText("");
            statsPanel.repaint();
        }
    }

    // --- Loan Features (User) ---

    private void requestLoan() {
        if (loggedInUser == null || loggedInUser.getUsername().equals(ADMIN_USER)) {
            bankingStatusLabel.setText("Loan requests are for regular users only.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        String amtText = loanRequestField.getText().trim();
        try {
            double amount = Double.parseDouble(amtText);

            if (amount <= 0) {
                bankingStatusLabel.setText("Loan amount must be positive.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (!Double.isFinite(amount) || amount > MAX_TRANSACTION_LIMIT) {
                bankingStatusLabel.setText("Invalid or excessive loan amount.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            double availableToBorrow = globalLoanLimit - loggedInUser.getLoanBalance();
            if (amount > availableToBorrow) {
                bankingStatusLabel.setText(String.format("Loan denied: exceeds your available loan limit ($%.2f available).", availableToBorrow));
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            loggedInUser.requestLoan(amount);
            saveData();
            updateBankingUI();
            bankingStatusLabel.setText(String.format("Loan approved: $%.2f credited. Loan balance: $%.2f", amount, loggedInUser.getLoanBalance()));
            bankingStatusLabel.setForeground(new Color(0, 128, 0));
        } catch (NumberFormatException ex) {
            bankingStatusLabel.setText("Invalid loan amount. Please enter numbers.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            loanRequestField.setText("");
            statsPanel.repaint();
        }
    }

    private void repayLoan() {
        if (loggedInUser == null || loggedInUser.getUsername().equals(ADMIN_USER)) {
            bankingStatusLabel.setText("Loan repayment is for regular users only.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        String amtText = loanRepayField.getText().trim();
        try {
            double amount = Double.parseDouble(amtText);

            if (amount <= 0) {
                bankingStatusLabel.setText("Repayment amount must be positive.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (!Double.isFinite(amount) || amount > MAX_TRANSACTION_LIMIT) {
                bankingStatusLabel.setText("Invalid or excessive repayment amount.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (loggedInUser.getBalance() < amount) {
                bankingStatusLabel.setText("Insufficient funds to repay loan. Deposit more or withdraw less.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            double repaid = loggedInUser.repayLoan(amount);
            saveData();
            updateBankingUI();
            bankingStatusLabel.setText(String.format("Repayment successful: $%.2f paid toward loan. Remaining loan: $%.2f", repaid, loggedInUser.getLoanBalance()));
            bankingStatusLabel.setForeground(new Color(0, 128, 0));
        } catch (NumberFormatException ex) {
            bankingStatusLabel.setText("Invalid repayment amount. Please enter numbers.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            loanRepayField.setText("");
            statsPanel.repaint();
        }
    }

    // --- Admin Logic ---

    private void adminGrantTransfer() {
        if (loggedInUser == null || !loggedInUser.getUsername().equals(ADMIN_USER)) {
            bankingStatusLabel.setText("ERROR: Must be logged in as Admin.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        String recipient = recipientUsernameField.getText().trim();
        String amountText = transferAmountField.getText().trim();
        double amount;

        if (recipient.isEmpty()) {
            bankingStatusLabel.setText("Recipient username cannot be empty.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        if (recipient.equals(ADMIN_USER)) {
            bankingStatusLabel.setText("Cannot transfer to the Admin account.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        User recipientUser = accounts.get(recipient);
        if (recipientUser == null) {
            bankingStatusLabel.setText("Error: Recipient user '" + recipient + "' does not exist.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        try {
            amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                bankingStatusLabel.setText("Grant amount must be positive.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            if (!Double.isFinite(amount) || amount > MAX_TRANSACTION_LIMIT) {
                bankingStatusLabel.setText(String.format("Invalid or excessive amount. Max is $%.0f.", MAX_TRANSACTION_LIMIT));
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            recipientUser.deposit(amount);
            saveData();
            updateUserList();

            bankingStatusLabel.setText(String.format("SUCCESS: Admin granted $%.2f to '%s'.", amount, recipient));
            bankingStatusLabel.setForeground(new Color(0, 128, 0));

        } catch (NumberFormatException e) {
            bankingStatusLabel.setText("Invalid amount entered. Please use numbers for the amount.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            recipientUsernameField.setText("");
            transferAmountField.setText("");
            statsPanel.repaint();
        }
    }

    private void resetSelectedBalance() {
        if (loggedInUser == null || !loggedInUser.getUsername().equals(ADMIN_USER)) {
            bankingStatusLabel.setText("ERROR: Must be logged in as Admin to reset.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        String selectedLine = userList.getSelectedValue();
        if (selectedLine == null || selectedLine.contains("No user accounts") || selectedLine.contains("USERNAME")) {
            JOptionPane.showMessageDialog(this, "Please select a user to reset their balance.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String usernameToReset = selectedLine.split("\\s+")[0];

        int response = JOptionPane.showConfirmDialog(this,
                "Reset balance for '" + usernameToReset + "' to $0.00?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            User userToReset = accounts.get(usernameToReset);
            if (userToReset != null) {
                userToReset.setBalance(0.0);
                saveData();
                updateUserList();
                bankingStatusLabel.setText("SUCCESS: Balance reset to $0.00.");
                bankingStatusLabel.setForeground(new Color(255, 69, 0));
            } else {
                bankingStatusLabel.setText("ERROR: User not found.");
                bankingStatusLabel.setForeground(Color.RED);
            }
        } else {
            bankingStatusLabel.setText("Balance reset cancelled.");
            bankingStatusLabel.setForeground(Color.BLUE);
        }
        statsPanel.repaint();
    }

    private void applyLoanSettings() {
        if (loggedInUser == null || !loggedInUser.getUsername().equals(ADMIN_USER)) {
            bankingStatusLabel.setText("ERROR: Must be logged in as Admin to change loan settings.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        try {
            double limit = Double.parseDouble(adminLoanLimitField.getText().trim());
            double rate = Double.parseDouble(adminLoanRateField.getText().trim());

            if (limit < 0 || !Double.isFinite(limit)) {
                bankingStatusLabel.setText("Invalid loan limit.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }
            if (rate < 0 || !Double.isFinite(rate)) {
                bankingStatusLabel.setText("Invalid loan interest rate.");
                bankingStatusLabel.setForeground(Color.RED);
                return;
            }

            globalLoanLimit = limit;
            globalLoanInterestRate = rate;
            bankingStatusLabel.setText(String.format("Loan settings applied: limit $%.2f, rate %.6f/sec", globalLoanLimit, globalLoanInterestRate));
            bankingStatusLabel.setForeground(new Color(0, 128, 0));
            saveData();
            updateUserList();
        } catch (NumberFormatException e) {
            bankingStatusLabel.setText("Invalid loan settings. Enter numeric values.");
            bankingStatusLabel.setForeground(Color.RED);
        } finally {
            statsPanel.repaint();
        }
    }

    private void forgiveSelectedUserLoan() {
        if (loggedInUser == null || !loggedInUser.getUsername().equals(ADMIN_USER)) {
            bankingStatusLabel.setText("ERROR: Must be logged in as Admin to forgive loans.");
            bankingStatusLabel.setForeground(Color.RED);
            return;
        }

        String selectedLine = userList.getSelectedValue();
        if (selectedLine == null || selectedLine.contains("No user accounts") || selectedLine.contains("USERNAME")) {
            JOptionPane.showMessageDialog(this, "Please select a user to forgive their loan.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = selectedLine.split("\\s+")[0];

        int response = JOptionPane.showConfirmDialog(this,
                "Forgive loan for '" + username + "'?",
                "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            User user = accounts.get(username);
            if (user != null) {
                user.setLoanBalance(0.0);
                saveData();
                updateUserList();
                bankingStatusLabel.setText("SUCCESS: Loan forgiven for '" + username + "'.");
                bankingStatusLabel.setForeground(new Color(0, 128, 0));
            } else {
                bankingStatusLabel.setText("ERROR: User not found after selection.");
                bankingStatusLabel.setForeground(Color.RED);
            }
        } else {
            bankingStatusLabel.setText("Loan forgiveness cancelled.");
            bankingStatusLabel.setForeground(Color.BLUE);
        }
        statsPanel.repaint();
    }

    private void logout() {
        loggedInUser = null;
        toggleAdminFeatures(false);
        cardLayout.show(mainPanel, "AUTH");
        authStatusLabel.setText("Logged out successfully.");
        authStatusLabel.setForeground(Color.BLUE);
        bankingStatusLabel.setText("Ready for transaction.");
        bankingStatusLabel.setForeground(Color.BLACK);
    }

    // --- UI Update and Interest/Debt Service ---

    private void toggleAdminFeatures(boolean isAdmin) {
        CardLayout cl = (CardLayout) (centerViewContainer.getLayout());

        if (isAdmin) {
            cl.show(centerViewContainer, "ADMIN_VIEW");
            welcomeLabel.setText("Welcome, ADMIN!");
            balanceLabel.setText("ADMIN CONTROL PANEL");
            balanceLabel.setForeground(new Color(138, 43, 226));
            loanLabel.setText(String.format("Global Loan Limit: $%.2f  |  Rate/sec: %.6f", globalLoanLimit, globalLoanInterestRate));
            bankingStatusLabel.setText("ADMIN MODE: Select a user to manage.");
            bankingStatusLabel.setForeground(Color.MAGENTA);

            adminLoanLimitField.setText(String.format("%.2f", globalLoanLimit));
            adminLoanRateField.setText(String.format("%.6f", globalLoanInterestRate));

            updateUserList();
        } else {
            cl.show(centerViewContainer, "USER_VIEW");
            updateBankingUI();
            bankingStatusLabel.setText("Ready for transaction.");
            bankingStatusLabel.setForeground(Color.BLACK);
        }
        statsPanel.repaint();
    }

    /** Populates the JList with current user accounts and balances (Admin only) */
    private void updateUserList() {
        if (loggedInUser == null || !loggedInUser.getUsername().equals(ADMIN_USER)) return;

        int selectedIndex = userList.getSelectedIndex();
        listModel.clear();

        listModel.addElement(String.format("%-12s %12s %12s", "USERNAME", "BALANCE", "LOAN"));
        listModel.addElement(String.format("%-12s %12s %12s", "--------", "-------", "----"));

        if (accounts.isEmpty()) {
            listModel.addElement("No user accounts exist.");
            resetButton.setEnabled(false);
            selectedIndex = -1;
        }

        accounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    User user = entry.getValue();
                    String formattedBalance = String.format("$%.2f", user.getBalance());
                    String formattedLoan = String.format("$%.2f", user.getLoanBalance());
                    String line = String.format("%-12s %12s %12s", user.getUsername(), formattedBalance, formattedLoan);
                    listModel.addElement(line);
                });

        if (selectedIndex != -1 && selectedIndex < listModel.getSize()) {
            userList.setSelectedIndex(selectedIndex);
        }
        resetButton.setEnabled(true);
        statsPanel.repaint();
    }

    private void updateBankingUI() {
        if (loggedInUser != null) {
            if (loggedInUser.getUsername().equals(ADMIN_USER)) return;

            welcomeLabel.setText("Welcome, " + loggedInUser.getUsername() + "!");
            balanceLabel.setText(String.format("Balance: $%.2f", loggedInUser.getBalance()));

            if (loggedInUser.getBalance() >= 0) {
                balanceLabel.setForeground(new Color(46, 139, 87));
            } else {
                balanceLabel.setForeground(Color.RED);
            }

            loanLabel.setText(String.format("Loan: $%.2f (Avail: $%.2f)",
                    loggedInUser.getLoanBalance(),
                    Math.max(0.0, globalLoanLimit - loggedInUser.getLoanBalance())));
        }
        statsPanel.repaint();
    }

    private void startInterestService() {
        final double INTEREST_RATE = 0.001; // 0.1% per second

        Runnable interestTask = () -> {
            boolean dataChanged = false;
            for (User user : accounts.values()) {
                if (user.getBalance() > 0) {
                    user.addInterest(INTEREST_RATE);
                    dataChanged = true;
                } else if (user.getBalance() < 0) {
                    user.chargeDebtFee(DEBT_FEE_RATE);
                    dataChanged = true;
                }
                if (user.getLoanBalance() > 0) {
                    user.accrueLoanInterest(globalLoanInterestRate);
                    dataChanged = true;
                }
            }

            if (dataChanged) saveData();

            if (loggedInUser != null) {
                if (!loggedInUser.getUsername().equals(ADMIN_USER)) {
                    SwingUtilities.invokeLater(this::updateBankingUI);
                } else {
                    SwingUtilities.invokeLater(this::updateUserList);
                }
            } else {
                // no user logged in, still repaint stats
                SwingUtilities.invokeLater(statsPanel::repaint);
            }
        };

        interestScheduler.scheduleAtFixedRate(interestTask, 1, 1, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::exitProgram));
    }

    // --- Persistence and Exit Methods ---

    private void exitProgram() {
        System.out.println("Shutting down interest scheduler and saving data...");
        interestScheduler.shutdown();
        saveData();
    }

    private void saveData() {
        try (FileOutputStream fos = new FileOutputStream(DATA_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(accounts);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                accounts = (Map<String, User>) ois.readObject();
                System.out.println("Data loaded successfully from " + DATA_FILE);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading data. Starting fresh. " + e.getMessage());
            }
        } else {
            System.out.println("No existing data found. Starting fresh.");
        }
    }

    // --- Stats Panel (small) ---
    private class StatsPanel extends JPanel {
        public StatsPanel() {
            setBackground(new Color(245, 245, 255));
            setBorder(BorderFactory.createTitledBorder("Bank Stats"));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Calculate totals
            double totalPos = 0.0;
            double totalNeg = 0.0;
            double totalLoans = 0.0;
            int userCount = 0;
            for (User u : accounts.values()) {
                userCount++;
                double b = u.getBalance();
                if (b >= 0) totalPos += b;
                else totalNeg += Math.abs(b);
                totalLoans += u.getLoanBalance();
            }

            double total = totalPos + totalNeg + totalLoans;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Small pie chart area
            int w = Math.min(getWidth(), getHeight()) - 70;
            int size = Math.max(80, Math.min(200, w));
            int cx = 20;
            int cy = 20;

            int x = cx;
            int y = cy;
            int diameter = size;

            if (total <= 0.0001) {
                // draw empty pie with grey
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillOval(x, y, diameter, diameter);
            } else {
                int start = 0;
                // positive balances slice - green
                int anglePos = (int) Math.round((totalPos / total) * 360);
                g2.setColor(new Color(46, 139, 87));
                g2.fillArc(x, y, diameter, diameter, start, anglePos);
                start += anglePos;

                // negative balances slice - red
                int angleNeg = (int) Math.round((totalNeg / total) * 360);
                g2.setColor(new Color(220, 20, 60));
                g2.fillArc(x, y, diameter, diameter, start, angleNeg);
                start += angleNeg;

                // loans slice - orange
                int angleLoan = 360 - (anglePos + angleNeg);
                if (angleLoan > 0) {
                    g2.setColor(new Color(255, 165, 0));
                    g2.fillArc(x, y, diameter, diameter, start, angleLoan);
                }
            }

            // Legend & stats text
            int tx = x + diameter + 10;
            int ty = y + 10;
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("Users: " + userCount, tx, ty);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            ty += 18;
            g2.setColor(new Color(46, 139, 87));
            g2.fillRect(tx, ty - 12, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString("Total Positive: $" + String.format("%.2f", totalPos), tx + 16, ty);

            ty += 16;
            g2.setColor(new Color(220, 20, 60));
            g2.fillRect(tx, ty - 12, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString("Total Debt:     $" + String.format("%.2f", totalNeg), tx + 16, ty);

            ty += 16;
            g2.setColor(new Color(255, 165, 0));
            g2.fillRect(tx, ty - 12, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString("Outstanding Loans: $" + String.format("%.2f", totalLoans), tx + 16, ty);

            // Footer small note
            g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g2.drawString("Pie = pos / debt / loans", x, y + diameter + 20);
        }
    }

    // --- Main Method ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankingApp app = new BankingApp();
            app.setVisible(true);
        });
    }

    // --- User Class ---

    private static class User implements Serializable {
        private static final long serialVersionUID = 3L;
        private String username;
        private String password;
        private double balance;
        private double loanBalance;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
            this.balance = 0.0;
            this.loanBalance = 0.0;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public double getBalance() { return balance; }
        public double getLoanBalance() { return loanBalance; }
        public void setLoanBalance(double loanBalance) { this.loanBalance = loanBalance; }
        public void setBalance(double balance) { this.balance = balance; }

        public void deposit(double amount) {
            if (amount > 0) this.balance += amount;
        }

        public boolean withdraw(double amount) {
            if (amount > 0) {
                this.balance -= amount;
                return true;
            }
            return false;
        }

        public synchronized void addInterest(double rate) {
            if (this.balance > 0) {
                double interest = this.balance * rate;
                this.balance += interest;
            }
        }

        public synchronized void chargeDebtFee(double rate) {
            if (this.balance < 0) {
                double debtMagnitude = Math.abs(this.balance);
                double fee = debtMagnitude * rate;
                this.balance -= fee;
            }
        }

        public synchronized void requestLoan(double amount) {
            if (amount > 0) {
                this.loanBalance += amount;
                this.balance += amount;
            }
        }

        public synchronized double repayLoan(double amount) {
            if (amount <= 0) return 0.0;
            double amountToApply = Math.min(amount, this.loanBalance);
            this.loanBalance -= amountToApply;
            this.balance -= amountToApply;
            return amountToApply;
        }

        public synchronized void accrueLoanInterest(double rate) {
            if (this.loanBalance > 0) {
                double interest = this.loanBalance * rate;
                this.loanBalance += interest;
            }
        }
    }
}
