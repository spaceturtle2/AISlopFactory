import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.Base64;

/**
 * Single-file user + user-store implementation.
 *
 * Usage:
 *  - User.addUser(username, password)
 *  - User.findByUsername(username)
 *  - User.getUsers()
 *  - User.saveToFile(File) / User.loadFromFile(File)
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // per-user data
    private String username;
    private double balance;
    private String passwordHashBase64;
    private String saltBase64;

    // in-memory store
    private static final List<User> USERS = Collections.synchronizedList(new ArrayList<>());

    public User(String username, String rawPassword) {
        this.username = username;
        this.balance = 0.0;
        byte[] salt = generateSalt();
        this.saltBase64 = Base64.getEncoder().encodeToString(salt);
        this.passwordHashBase64 = Base64.getEncoder().encodeToString(hash(rawPassword, salt));
    }

    // Instance API
    public String getUsername() { return username; }
    public double getBalance() { return balance; }

    public synchronized void deposit(double amt) {
        if (amt > 0) balance += amt;
    }

    public synchronized boolean withdraw(double amt) {
        if (amt > 0 && amt <= balance) {
            balance -= amt;
            return true;
        }
        return false;
    }

    public boolean authenticate(String attemptedPassword) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] attemptedHash = hash(attemptedPassword, salt);
        String attemptedHashBase64 = Base64.getEncoder().encodeToString(attemptedHash);
        return attemptedHashBase64.equals(passwordHashBase64);
    }

    public void setPassword(String newPassword) {
        byte[] salt = generateSalt();
        this.saltBase64 = Base64.getEncoder().encodeToString(salt);
        this.passwordHashBase64 = Base64.getEncoder().encodeToString(hash(newPassword, salt));
    }

    @Override
    public String toString() {
        return username + " (Balance: $" + String.format("%.2f", balance) + ")";
    }

    // Static store API
    public static List<User> getUsers() {
        synchronized (USERS) {
            return new ArrayList<>(USERS);
        }
    }

    public static Optional<User> findByUsername(String username) {
        synchronized (USERS) {
            return USERS.stream()
                    .filter(u -> u.username.equalsIgnoreCase(username))
                    .findFirst();
        }
    }

    public static boolean addUser(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty()) return false;
        String n = username.trim();
        synchronized (USERS) {
            boolean exists = USERS.stream().anyMatch(u -> u.username.equalsIgnoreCase(n));
            if (exists) return false;
            USERS.add(new User(n, rawPassword));
            return true;
        }
    }

    public static boolean removeUser(String username) {
        synchronized (USERS) {
            Iterator<User> it = USERS.iterator();
            while (it.hasNext()) {
                User u = it.next();
                if (u.username.equalsIgnoreCase(username)) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean transfer(String fromUsername, String toUsername, double amount) {
        if (amount <= 0) return false;
        synchronized (USERS) {
            Optional<User> fromOpt = USERS.stream().filter(u -> u.username.equalsIgnoreCase(fromUsername)).findFirst();
            Optional<User> toOpt = USERS.stream().filter(u -> u.username.equalsIgnoreCase(toUsername)).findFirst();
            if (fromOpt.isEmpty() || toOpt.isEmpty()) return false;
            User from = fromOpt.get();
            User to = toOpt.get();
            // lock order by username to avoid deadlock
            User a = (from.username.compareToIgnoreCase(to.username) <= 0) ? from : to;
            User b = (a == from) ? to : from;
            synchronized (a) {
                synchronized (b) {
                    if (from.withdraw(amount)) {
                        to.deposit(amount);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    public static String[] usernames() {
        synchronized (USERS) {
            return USERS.stream().map(u -> u.username).toArray(String[]::new);
        }
    }

    // Persistence: serialize an ArrayList<User>
    public static void saveToFile(File f) throws IOException {
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        synchronized (USERS) {
            ArrayList<User> snapshot = new ArrayList<>(USERS);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(snapshot);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadFromFile(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object o = ois.readObject();
            if (!(o instanceof ArrayList)) {
                throw new IOException("File does not contain user list");
            }
            ArrayList<User> loaded = (ArrayList<User>) o;
            synchronized (USERS) {
                USERS.clear();
                USERS.addAll(loaded);
            }
        }
    }

    // Helpers
    private static byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[16];
        rnd.nextBytes(salt);
        return salt;
    }

    private static byte[] hash(String rawPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(rawPassword.getBytes());
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static void ensureDefaultAdmin() {
        synchronized (USERS) {
            if (USERS.isEmpty()) {
                addUser("admin", "admin123");
            }
        }
    }
}
