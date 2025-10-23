// ListUsers.java
import java.util.Map;

public class ListUsers {
    public static void main(String[] args) {
        UserStore store = new UserStore(); // ./users
        Map<String, User> all = store.loadAll();
        System.out.println("Runtime users: " + all.keySet());
        for (User u : all.values()) {
            System.out.println(u);
            System.out.println("verify known password? " + (u.getUsername().equals("alice") ? u.verifyPassword("password123") : u.verifyPassword("s3cr3t")));
        }
    }
}
