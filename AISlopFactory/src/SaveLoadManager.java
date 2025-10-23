import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Simple Swing GUI to save/load snapshots of all users into "src/saves/slotN.json".
 * - Writes readable JSON that you can commit to GitHub.
 * - Loads JSON snapshots and writes users back to runtime store (./users/*.dat).
 *
 * NOTE: For robustness in real projects swap crude JSON with a proper library (Gson/Jackson).
 */
public class SaveLoadManager extends JFrame {

    private static final int SLOT_COUNT = 3;
    private final JComboBox<String> slotCombo;
    private final JButton saveBtn;
    private final JButton loadBtn;
    private final JTextArea logArea;

    private final Path savesDir = Paths.get("src", "saves");
    private final UserStore userStore = new UserStore(); // uses ./users/

    public SaveLoadManager() {
        super("Save/Load Manager - AI Slop Factory");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 420);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        slotCombo = new JComboBox<>();
        for (int i = 1; i <= SLOT_COUNT; i++) slotCombo.addItem("Slot " + i);
        saveBtn = new JButton("Save to Slot");
        loadBtn = new JButton("Load from Slot");
        top.add(new JLabel("Choose slot:"));
        top.add(slotCombo);
        top.add(saveBtn);
        top.add(loadBtn);
        add(top, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        saveBtn.addActionListener(this::onSave);
        loadBtn.addActionListener(this::onLoad);

        try {
            Files.createDirectories(savesDir);
            appendLog("Saves directory: " + savesDir.toAbsolutePath());
        } catch (IOException e) {
            appendLog("Could not create saves directory: " + e.getMessage());
        }
    }

    private void appendLog(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void onSave(ActionEvent ev) {
        int slot = slotCombo.getSelectedIndex() + 1;
        Path slotFile = savesDir.resolve("slot" + slot + ".json");
        appendLog("Saving users -> " + slotFile);

        Map<String, User> users = userStore.loadAll();
        if (users == null || users.isEmpty()) {
            appendLog("No users found in runtime store.");
            return;
        }

        try (BufferedWriter w = Files.newBufferedWriter(slotFile)) {
            w.write("{\n");
            w.write("  \"generatedAt\": \"" + new Date().toString() + "\",\n");
            w.write("  \"users\": [\n");
            boolean firstUser = true;
            for (User u : users.values()) {
                if (!firstUser) w.write(",\n");
                firstUser = false;
                writeUserJson(w, u, "    ");
            }
            w.write("\n  ]\n");
            w.write("}\n");
            appendLog("Saved " + users.size() + " users to " + slotFile);
        } catch (IOException e) {
            appendLog("Save failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeUserJson(Writer w, User u, String indent) throws IOException {
        String username = safe(u.getUsername());
        String hash = safeNullable(u.getPasswordHash());
        String salt = safeNullable(u.getPasswordSalt());
        double balance = u.getBalance();
        double loan = u.getLoanBalance();
        Map<String, Integer> portfolio = u.getPortfolio();

        w.write(indent + "{\n");
        w.write(indent + "  \"username\": \"" + username + "\",\n");
        w.write(indent + "  \"passwordHash\": \"" + hash + "\",\n");
        w.write(indent + "  \"passwordSalt\": \"" + salt + "\",\n");
        w.write(indent + "  \"balance\": " + balance + ",\n");
        w.write(indent + "  \"loanBalance\": " + loan + ",\n");
        w.write(indent + "  \"portfolio\": {\n");

        boolean first = true;
        for (Map.Entry<String, Integer> e : portfolio.entrySet()) {
            if (!first) w.write(",\n");
            first = false;
            w.write(indent + "    \"" + safe(e.getKey()) + "\": " + e.getValue());
        }
        w.write("\n" + indent + "  }\n");
        w.write(indent + "}");
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    private String safeNullable(String s) {
        if (s == null) return "";
        return safe(s);
    }

    private void onLoad(ActionEvent ev) {
        int slot = slotCombo.getSelectedIndex() + 1;
        Path slotFile = savesDir.resolve("slot" + slot + ".json");

        if (!Files.exists(slotFile)) {
            appendLog("Slot file does not exist: " + slotFile);
            return;
        }

        appendLog("Loading users from " + slotFile + " ...");

        try {
            String content = Files.readString(slotFile);
            Map<String, Map<String, Object>> parsed = parseSlotJson(content);
            if (parsed == null || parsed.isEmpty()) {
                appendLog("No users found in slot file.");
                return;
            }

            int count = 0;
            for (Map<String, Object> userMap : parsed.values()) {
                User u = userFromMap(userMap);
                if (u != null) {
                    boolean ok = userStore.save(u);
                    if (ok) count++;
                    else appendLog("Failed to save user: " + u.getUsername());
                }
            }
            appendLog("Loaded " + count + " users into runtime store.");
        } catch (IOException e) {
            appendLog("Load failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** minimal parser targeted to the JSON format written by this tool */
    private Map<String, Map<String, Object>> parseSlotJson(String json) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        int usersIdx = json.indexOf("\"users\"");
        if (usersIdx < 0) return out;
        int startArray = json.indexOf('[', usersIdx);
        int endArray = json.indexOf(']', startArray);
        if (startArray < 0 || endArray < 0) return out;
        String arrayBody = json.substring(startArray + 1, endArray);

        String[] items = arrayBody.split("\\},\\s*\\{");
        for (String itemRaw : items) {
            String item = itemRaw.trim();
            if (!item.startsWith("{")) item = "{" + item;
            if (!item.endsWith("}")) item = item + "}";
            Map<String, Object> m = parseUserObject(item);
            if (m != null && m.containsKey("username")) {
                out.put((String)m.get("username"), m);
            }
        }
        return out;
    }

    private Map<String, Object> parseUserObject(String obj) {
        Map<String, Object> m = new HashMap<>();
        try (Scanner sc = new Scanner(obj)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (!line.contains(":")) continue;
                if (line.endsWith(",")) line = line.substring(0, line.length()-1);
                int colon = line.indexOf(':');
                String k = line.substring(0, colon).trim().replaceAll("^\"|\"$", "");
                String v = line.substring(colon+1).trim();

                if (v.startsWith("\"")) {
                    v = v.replaceAll("^\"|\"$", "");
                    m.put(k, v);
                } else if (v.startsWith("{")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(v).append("\n");
                    while (sc.hasNextLine()) {
                        String l2 = sc.nextLine();
                        sb.append(l2).append("\n");
                        if (l2.trim().equals("}")) break;
                    }
                    Map<String,Integer> portfolio = new HashMap<>();
                    String pb = sb.toString();
                    Scanner sc2 = new Scanner(pb);
                    while (sc2.hasNextLine()) {
                        String pl = sc2.nextLine().trim();
                        if (!pl.contains(":")) continue;
                        if (pl.endsWith(",")) pl = pl.substring(0, pl.length()-1);
                        String[] kv = pl.split(":", 2);
                        if (kv.length != 2) continue;
                        String pk = kv[0].trim().replaceAll("^\"|\"$", "");
                        String pv = kv[1].trim().replaceAll("^,|,$", "");
                        try { portfolio.put(pk, Integer.parseInt(pv)); } catch (NumberFormatException ignored) {}
                    }
                    m.put(k, portfolio);
                } else {
                    try {
                        if (v.contains(".")) m.put(k, Double.parseDouble(v));
                        else m.put(k, Integer.parseInt(v));
                    } catch (NumberFormatException e) {
                        m.put(k, v);
                    }
                }
            }
            return m;
        } catch (Exception e) {
            appendLog("Failed to parse user object: " + e.getMessage());
            return null;
        }
    }

    private User userFromMap(Map<String, Object> map) {
        try {
            String username = (String) map.get("username");
            String hash = (String) map.get("passwordHash");
            String salt = (String) map.get("passwordSalt");
            double balance = map.containsKey("balance") ? ((Number) map.get("balance")).doubleValue() : 0.0;
            double loan = map.containsKey("loanBalance") ? ((Number) map.get("loanBalance")).doubleValue() : 0.0;
            Map<String, Integer> portfolio = (Map<String, Integer>) map.getOrDefault("portfolio", new HashMap<>());

            User u = new User();
            u.setUsername(username);
            u.setPasswordHashDirect(hash);
            u.setPasswordSaltDirect(salt);
            u.setBalance(balance);
            u.setLoanBalance(loan);
            u.setPortfolio(portfolio);
            return u;
        } catch (Exception e) {
            appendLog("Failed to build User from map: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SaveLoadManager m = new SaveLoadManager();
            m.setVisible(true);
        });
    }
}
