import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class BankingApp {
    private JFrame frame;
    private User currentUser;

    private static final String SAVE_FOLDER = "src";
    private static final String SAVE_FILE_PREFIX = "saveslot";
    private static final String SAVE_FILE_SUFFIX = ".dat";

    private DefaultListModel<String> listModel;
    private JList<String> userList;
    private JLabel statusLabel;
    private JComboBox<String> saveSlotCombo;

    public BankingApp() {
        buildGui();
    }

    private void buildGui() {
        frame = new JFrame("BankingApp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(860, 560);
        frame.setLocationRelativeTo(null);

        JPanel left = new JPanel(new BorderLayout(6,6));
        left.setBorder(BorderFactory.createTitledBorder("Users"));

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane listPane = new JScrollPane(userList);
        left.add(listPane, BorderLayout.CENTER);

        JPanel leftButtons = new JPanel(new GridLayout(0,1,4,4));
        JButton refreshBtn = new JButton("Refresh list");
        refreshBtn.addActionListener(e -> refreshUserList());
        leftButtons.add(refreshBtn);

        JButton viewBtn = new JButton("View selected");
        viewBtn.addActionListener(e -> viewSelected());
        leftButtons.add(viewBtn);

        left.add(leftButtons, BorderLayout.SOUTH);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Create user panel
        JPanel createPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        createPanel.setBorder(BorderFactory.createTitledBorder("Create user"));
        JTextField newUserField = new JTextField(12);
        JPasswordField newPassField = new JPasswordField(12);
        JButton createBtn = new JButton("Create");
        createBtn.addActionListener(e -> {
            String u = newUserField.getText().trim();
            String p = new String(newPassField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter username and password.");
                return;
            }
            boolean ok = User.addUser(u, p);
            if (ok) {
                newUserField.setText("");
                newPassField.setText("");
                refreshUserList();
                setStatus("User '" + u + "' created.");
            } else {
                JOptionPane.showMessageDialog(frame, "Could not create user (exists?).");
            }
        });
        createPanel.add(new JLabel("Username:"));
        createPanel.add(newUserField);
        createPanel.add(new JLabel("Password:"));
        createPanel.add(newPassField);
        createPanel.add(createBtn);
        right.add(createPanel);

        // Login panel
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Login"));
        JTextField loginUser = new JTextField(12);
        JPasswordField loginPass = new JPasswordField(12);
        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            String u = loginUser.getText().trim();
            String p = new String(loginPass.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter username and password to login.");
                return;
            }
            User.findByUsername(u).ifPresentOrElse(user -> {
                if (user.authenticate(p)) {
                    currentUser = user;
                    setStatus("Logged in as " + currentUser.getUsername());
                    refreshUserList();
                } else {
                    JOptionPane.showMessageDialog(frame, "Bad password.");
                }
            }, () -> JOptionPane.showMessageDialog(frame, "No such user."));
        });
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(loginUser);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(loginPass);
        loginPanel.add(loginBtn);
        right.add(loginPanel);

        // Transactions panel
        JPanel moneyPanel = new JPanel(new GridLayout(3,1,6,6));
        moneyPanel.setBorder(BorderFactory.createTitledBorder("Transactions"));

        // Deposit
        JPanel depositRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField depositAmt = new JTextField(8);
        JButton depositBtn = new JButton("Deposit");
        depositBtn.addActionListener(e -> {
            if (!ensureLoggedIn()) return;
            try {
                double amt = Double.parseDouble(depositAmt.getText().trim());
                if (amt <= 0) throw new NumberFormatException();
                currentUser.deposit(amt);
                setStatus("Deposited $" + String.format("%.2f", amt) + " to " + currentUser.getUsername());
                refreshUserList();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter a positive number.");
            }
        });
        depositRow.add(new JLabel("Amount:"));
        depositRow.add(depositAmt);
        depositRow.add(depositBtn);
        moneyPanel.add(depositRow);

        // Withdraw
        JPanel withdrawRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField withdrawAmt = new JTextField(8);
        JButton withdrawBtn = new JButton("Withdraw");
        withdrawBtn.addActionListener(e -> {
            if (!ensureLoggedIn()) return;
            try {
                double amt = Double.parseDouble(withdrawAmt.getText().trim());
                if (amt <= 0) throw new NumberFormatException();
                boolean ok = currentUser.withdraw(amt);
                if (ok) {
                    setStatus("Withdrew $" + String.format("%.2f", amt) + " from " + currentUser.getUsername());
                } else {
                    JOptionPane.showMessageDialog(frame, "Insufficient funds.");
                }
                refreshUserList();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter a positive number.");
            }
        });
        withdrawRow.add(new JLabel("Amount:"));
        withdrawRow.add(withdrawAmt);
        withdrawRow.add(withdrawBtn);
        moneyPanel.add(withdrawRow);

        // Transfer
        JPanel transferRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField toUserField = new JTextField(10);
        JTextField transferAmt = new JTextField(8);
        JButton transferBtn = new JButton("Transfer");
        transferBtn.addActionListener(e -> {
            if (!ensureLoggedIn()) return;
            String to = toUserField.getText().trim();
            if (to.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter recipient username.");
                return;
            }
            try {
                double amt = Double.parseDouble(transferAmt.getText().trim());
                if (amt <= 0) throw new NumberFormatException();
                boolean ok = User.transfer(currentUser.getUsername(), to, amt);
                if (ok) {
                    setStatus("Transferred $" + String.format("%.2f", amt) + " to " + to);
                } else {
                    JOptionPane.showMessageDialog(frame, "Transfer failed (user not found or insufficient funds).");
                }
                refreshUserList();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter a positive number.");
            }
        });
        transferRow.add(new JLabel("To:"));
        transferRow.add(toUserField);
        transferRow.add(new JLabel("Amount:"));
        transferRow.add(transferAmt);
        transferRow.add(transferBtn);
        moneyPanel.add(transferRow);

        right.add(moneyPanel);

        // Save/Load slots
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        savePanel.setBorder(BorderFactory.createTitledBorder("Save / Load"));
        saveSlotCombo = new JComboBox<>(new String[]{"Slot 1", "Slot 2", "Slot 3"});
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveSlot());
        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> loadSlot());

        savePanel.add(new JLabel("Slot:"));
        savePanel.add(saveSlotCombo);
        savePanel.add(saveBtn);
        savePanel.add(loadBtn);

        JButton deleteSlotBtn = new JButton("Delete File");
        deleteSlotBtn.addActionListener(e -> deleteSlotFile());
        savePanel.add(deleteSlotBtn);

        right.add(savePanel);

        // Misc panel
        JPanel miscPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        miscPanel.setBorder(BorderFactory.createTitledBorder("Misc"));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            currentUser = null;
            setStatus("Logged out");
            refreshUserList();
        });
        JButton changePassBtn = new JButton("Change Password");
        changePassBtn.addActionListener(e -> {
            if (!ensureLoggedIn()) return;
            JPasswordField pf = new JPasswordField();
            int res = JOptionPane.showConfirmDialog(frame, pf, "Enter new password", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String newP = new String(pf.getPassword());
                if (!newP.isEmpty()) {
                    currentUser.setPassword(newP);
                    setStatus("Password changed for " + currentUser.getUsername());
                } else {
                    JOptionPane.showMessageDialog(frame, "Password cannot be empty.");
                }
            }
        });
        JButton removeBtn = new JButton("Remove selected user");
        removeBtn.addActionListener(e -> {
            String sel = userList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(frame, "Select a user.");
                return;
            }
            String username = extractUsernameFromListEntry(sel);
            int confirm = JOptionPane.showConfirmDialog(frame, "Delete user " + username + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (User.removeUser(username)) {
                    setStatus("Removed " + username);
                    if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(username)) currentUser = null;
                    refreshUserList();
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to remove user.");
                }
            }
        });

        miscPanel.add(logoutBtn);
        miscPanel.add(changePassBtn);
        miscPanel.add(removeBtn);
        right.add(miscPanel);

        // status
        statusLabel = new JLabel("Ready");
        right.add(statusLabel);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(320);

        frame.getContentPane().add(split);
        frame.setVisible(true);

        // default admin
        User.ensureDefaultAdmin();
        refreshUserList();
    }

    private boolean ensureLoggedIn() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(frame, "You must be logged in to do that.");
            return false;
        }
        return true;
    }

    private void refreshUserList() {
        listModel.clear();
        for (User u : User.getUsers()) {
            listModel.addElement(u.toString());
        }
    }

    private void viewSelected() {
        String sel = userList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(frame, "Select a user from the list.");
            return;
        }
        String username = extractUsernameFromListEntry(sel);
        User.findByUsername(username).ifPresent(u -> {
            JOptionPane.showMessageDialog(frame, "User: " + u.getUsername() + "\nBalance: $" + String.format("%.2f", u.getBalance()));
        });
    }

    private String extractUsernameFromListEntry(String listEntry) {
        // listEntry is formatted as "username (Balance: $X.XX)". This extracts the username part safely.
        int idx = listEntry.indexOf(" (");
        if (idx > 0) return listEntry.substring(0, idx);
        return listEntry;
    }

    private File getSaveFileForSelectedSlot() {
        int slot = saveSlotCombo.getSelectedIndex() + 1; // 1..3
        String fname = SAVE_FILE_PREFIX + slot + SAVE_FILE_SUFFIX;
        return new File(SAVE_FOLDER, fname);
    }

    private void saveSlot() {
        File f = getSaveFileForSelectedSlot();
        try {
            User.saveToFile(f);
            setStatus("Saved to " + f.getPath());
            JOptionPane.showMessageDialog(frame, "Saved to " + f.getPath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to save: " + ex.getMessage());
        }
    }

    private void loadSlot() {
        File f = getSaveFileForSelectedSlot();
        if (!f.exists()) {
            JOptionPane.showMessageDialog(frame, "Save file not found: " + f.getPath());
            return;
        }
        try {
            User.loadFromFile(f);
            currentUser = null;
            refreshUserList();
            setStatus("Loaded from " + f.getPath());
            JOptionPane.showMessageDialog(frame, "Loaded from " + f.getPath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Failed to load: " + ex.getMessage());
        }
    }

    private void deleteSlotFile() {
        File f = getSaveFileForSelectedSlot();
        if (!f.exists()) {
            JOptionPane.showMessageDialog(frame, "No file to delete: " + f.getPath());
            return;
        }
        int c = JOptionPane.showConfirmDialog(frame, "Delete file " + f.getPath() + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            if (f.delete()) {
                setStatus("Deleted " + f.getPath());
                JOptionPane.showMessageDialog(frame, "Deleted " + f.getPath());
            } else {
                JOptionPane.showMessageDialog(frame, "Could not delete file (maybe in use).");
            }
        }
    }

    private void setStatus(String s) {
        statusLabel.setText(s);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankingApp());
    }
}