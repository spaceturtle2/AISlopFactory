// TestSetup.java
public class TestSetup {
    public static void main(String[] args) {
        UserStore store = new UserStore(); // ./users
        User alice = new User("alice", "password123");
        alice.deposit(150.0);
        alice.addToPortfolio("TURTLE", 5);

        User bob = new User("bob", "s3cr3t");
        bob.deposit(50.0);
        bob.addToPortfolio("SLOP", 2);

        System.out.println("Saving alice and bob to runtime store...");
        System.out.println("alice saved: " + store.save(alice));
        System.out.println("bob saved: " + store.save(bob));
    }
}
