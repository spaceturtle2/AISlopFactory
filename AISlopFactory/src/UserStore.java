import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple helper to persist User objects to disk using Java serialization.
 * - baseDir defaults to "./users"
 * - filenames are "<safe-username>.dat"
 */
public final class UserStore {

    final Path baseDir;

    public UserStore() {
        this(Path.of("users"));
    }

    public UserStore(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Path getUserFile(String username) {
        String safe = username == null ? "unknown" : username.replaceAll("[^a-zA-Z0-9._-]", "_");
        return baseDir.resolve(safe + ".dat");
    }

    /**
     * Save a user to disk. Returns true on success.
     */
    public boolean save(User user) {
        try {
            Files.createDirectories(baseDir);
            Path target = getUserFile(user.getUsername());
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");

            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(temp))) {
                oos.writeObject(user);
                oos.flush();
            }

            // atomic move if supported
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a user by username, or null if not found or on error.
     */
    public User load(String username) {
        Path f = getUserFile(username);
        if (!Files.exists(f)) return null;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof User) return (User) obj;
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete a user file (if exists).
     */
    public boolean delete(String username) {
        try {
            Path f = getUserFile(username);
            return Files.deleteIfExists(f);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load all users from the runtime user directory into a map username -> User.
     */
    public Map<String, User> loadAll() {
        Map<String, User> out = new LinkedHashMap<>();
        try {
            Files.createDirectories(baseDir);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(baseDir, "*.dat")) {
                for (Path p : ds) {
                    try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(p))) {
                        Object obj = ois.readObject();
                        if (obj instanceof User) {
                            User u = (User) obj;
                            out.put(u.getUsername(), u);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to read user file " + p + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Utility: list usernames present in runtime store.
     */
    public List<String> listUsernames() {
        return loadAll().keySet().stream().collect(Collectors.toList());
    }
}
