import java.util.Random;
import java.util.Scanner;

public class CatSim {
    // Cat class definition
    static class Cat {
        private String name;
        private int happiness; 
        private int hunger;
        private int sleep;
        private int bathroom;
        private boolean inGulag;
        
        public Cat(String n) {
            this.name = n;
            this.happiness = 50;
            this.hunger = 70;
            this.sleep = 100;
            this.bathroom = 70;
            this.inGulag = false;
        }
        
        // Accessor methods
        public String getName() { return this.name; }
        public int getHappy() { return this.happiness; }
        public int getHunger() { return this.hunger; }
        public int getSleep() { return this.sleep; }
        public int getBathroom() { return this.bathroom; }
        public boolean isInGulag() { return this.inGulag; }
        
        // Modifier methods
        public void increaseHappiness(int amount) {
            this.happiness = Math.min(100, this.happiness + amount);
        }
        
        public void decreaseHappiness(int amount) {
            this.happiness = Math.max(0, this.happiness - amount);
        }
        
        public void feed(int amount) {
            this.hunger = Math.min(100, this.hunger + amount);
        }
        
        public void makeHungry(int amount) {
            this.hunger = Math.max(0, this.hunger - amount);
        }
        
        public void rest(int amount) {
            this.sleep = Math.min(100, this.sleep + amount);
        }
        
        public void tire(int amount) {
            this.sleep = Math.max(0, this.sleep - amount);
        }
        
        public void useBathroom(int amount) {
            this.bathroom = Math.min(100, this.bathroom + amount);
        }
        
        public void needBathroom(int amount) {
            this.bathroom = Math.max(0, this.bathroom - amount);
        }
        
        public void sendToGulag() {
            this.inGulag = true;
        }
        
        public void displayStatus() {
            System.out.println("\n════════════════════════════════════════");
            System.out.println("🐱 " + name + "'s Status:");
            System.out.println("════════════════════════════════════════");
            System.out.println("😊 Happiness:   " + happiness + "/100 " + getBar(happiness));
            System.out.println("🍖 Hunger:      " + hunger + "/100 " + getBar(hunger));
            System.out.println("😴 Sleep:       " + sleep + "/100 " + getBar(sleep));
            System.out.println("🚽 Bathroom:    " + bathroom + "/100 " + getBar(bathroom));
            System.out.println("════════════════════════════════════════");
            
            if (happiness <= 20) System.out.println("⚠️  " + name + " is very unhappy!");
            if (hunger <= 20) System.out.println("⚠️  " + name + " is starving!");
            if (sleep <= 20) System.out.println("⚠️  " + name + " is exhausted!");
            if (bathroom <= 20) System.out.println("⚠️  " + name + " needs to go NOW!");
            
            if (inGulag) {
                System.out.println("\n❌ " + name + " HAS BEEN SENT TO THE GULAG! ❌");
            }
        }
        
        private String getBar(int value) {
            StringBuilder bar = new StringBuilder("[");
            int bars = value / 5;
            for (int i = 0; i < 20; i++) {
                if (i < bars) {
                    bar.append("█");
                } else {
                    bar.append("░");
                }
            }
            bar.append("]");
            return bar.toString();
        }
        
        public boolean isAlive() {
            return !inGulag && happiness > 0;
        }
        
        public void updateDaily() {
            // Natural decay
            makeHungry(8);
            needBathroom(8);
            tire(10);
            
            // Happiness affected by other stats
            if (hunger < 30) decreaseHappiness(5);
            if (sleep < 30) decreaseHappiness(5);
            if (bathroom < 30) decreaseHappiness(5);
            
            // Check for Gulag conditions
            if (happiness <= 0 || hunger <= 0 || bathroom <= 5) {
                sendToGulag();
            }
        }
    }
    
    // Choice class definition
    static class Choice {
        private String situation;
        private String optionA;
        private String optionB;
        private Runnable effectA;
        private Runnable effectB;
        
        public Choice(String situation, String optionA, String optionB, 
                     Runnable effectA, Runnable effectB) {
            this.situation = situation;
            this.optionA = optionA;
            this.optionB = optionB;
            this.effectA = effectA;
            this.effectB = effectB;
        }
        
        public void present(Cat cat) {
            System.out.println("\n════════════════════════════════════════");
            System.out.println(situation.replace("{cat}", cat.getName()));
            System.out.println("════════════════════════════════════════");
            System.out.println("A) " + optionA);
            System.out.println("B) " + optionB);
            System.out.println("════════════════════════════════════════");
        }
        
        public void execute(char choice, Cat cat) {
            if (choice == 'A' || choice == 'a') {
                effectA.run();
            } else if (choice == 'B' || choice == 'b') {
                effectB.run();
            }
        }
    }
    
    // Main game class
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();
        
        System.out.println("════════════════════════════════════════");
        System.out.println("          🐱 CAT LAPSE GAME 🐱          ");
        System.out.println("════════════════════════════════════════");
        
        System.out.print("\nEnter your cat's name: ");
        String catName = scanner.nextLine();
        if (catName.trim().isEmpty()) {
            catName = "Whiskers";
        }
        
        Cat cat = new Cat(catName);
        int day = 1;
        
        // Create list of possible scenarios
        Choice[] scenarios = {
            new Choice(
                "{cat} is meowing loudly in the kitchen...",
                "Give {cat} some tuna (Hunger +30, Happiness +10)",
                "Give {cat} dry food (Hunger +15)",
                () -> {
                    cat.feed(30);
                    cat.increaseHappiness(10);
                    System.out.println("\n🐟 " + cat.getName() + " devours the tuna happily!");
                },
                () -> {
                    cat.feed(15);
                    System.out.println("\n🥣 " + cat.getName() + " nibbles on the dry food.");
                }
            ),
            new Choice(
                "{cat} is looking at the door, then at you...",
                "Take {cat} outside (Happiness +25, Sleep -15)",
                "Play with {cat} indoors (Happiness +15, Sleep -10)",
                () -> {
                    cat.increaseHappiness(25);
                    cat.tire(15);
                    System.out.println("\n🌳 " + cat.getName() + " explores outside excitedly!");
                },
                () -> {
                    cat.increaseHappiness(15);
                    cat.tire(10);
                    System.out.println("\n🎮 You play with " + cat.getName() + " with a feather toy!");
                }
            ),
            new Choice(
                "{cat} is scratching at the litter box...",
                "Clean the litter box now (Bathroom +40, Happiness +5)",
                "Clean it later (Bathroom +20, Happiness -10)",
                () -> {
                    cat.useBathroom(40);
                    cat.increaseHappiness(5);
                    System.out.println("\n🧹 You clean the litter box. " + cat.getName() + " seems pleased!");
                },
                () -> {
                    cat.useBathroom(20);
                    cat.decreaseHappiness(10);
                    System.out.println("\n😾 " + cat.getName() + " gives you a dirty look.");
                }
            ),
            new Choice(
                "{cat} is napping on your keyboard...",
                "Let {cat} sleep (Sleep +25, Hunger -10)",
                "Move {cat} and work (Sleep -15, Happiness -15)",
                () -> {
                    cat.rest(25);
                    cat.makeHungry(10);
                    System.out.println("\n😴 You let " + cat.getName() + " sleep peacefully.");
                },
                () -> {
                    cat.tire(15);
                    cat.decreaseHappiness(15);
                    System.out.println("\n💻 You move " + cat.getName() + " and they seem annoyed.");
                }
            ),
            new Choice(
                "{cat} is chasing their tail in circles...",
                "Film {cat} for social media (Happiness +20)",
                "Stop {cat} before they get dizzy (Sleep +10, Happiness -5)",
                () -> {
                    cat.increaseHappiness(20);
                    System.out.println("\n📸 You film " + cat.getName() + " being silly. It goes viral!");
                },
                () -> {
                    cat.rest(10);
                    cat.decreaseHappiness(5);
                    System.out.println("\n🛑 You stop " + cat.getName() + ". They look confused.");
                }
            ),
            new Choice(
                "You hear a crash in the other room...",
                "Check on {cat} immediately (Happiness +5, finds broken vase)",
                "Ignore it and hope for the best (Happiness -10, Hunger -10)",
                () -> {
                    cat.increaseHappiness(5);
                    cat.makeHungry(10);
                    System.out.println("\n🏺 " + cat.getName() + " knocked over a vase! But they're glad you checked.");
                },
                () -> {
                    cat.decreaseHappiness(10);
                    cat.makeHungry(10);
                    System.out.println("\n🙈 You find your favorite mug broken. " + cat.getName() + " looks innocent.");
                }
            )
        };
        
        System.out.println("\nWelcome, " + catName + "'s caretaker!");
        System.out.println("Keep your cat happy and out of the GULAG!");
        System.out.println("Press Enter to begin...");
        scanner.nextLine();
        
        // Main game loop
        while (cat.isAlive()) {
            System.out.println("\n════════════════════════════════════════");
            System.out.println("                DAY " + day + "                 ");
            System.out.println("════════════════════════════════════════");
            
            // Show cat status
            cat.displayStatus();
            
            // Present random choice
            Choice currentChoice = scenarios[random.nextInt(scenarios.length)];
            currentChoice.present(cat);
            
            // Get player choice
            char playerChoice;
            while (true) {
                System.out.print("Your choice (A/B): ");
                String input = scanner.nextLine().toUpperCase();
                if (input.length() > 0 && (input.charAt(0) == 'A' || input.charAt(0) == 'B')) {
                    playerChoice = input.charAt(0);
                    break;
                }
                System.out.println("❌ Please enter A or B!");
            }
            
            // Execute choice
            currentChoice.execute(playerChoice, cat);
            
            // Apply daily updates
            cat.updateDaily();
            
            // Check if cat went to gulag
            if (cat.isInGulag()) {
                System.out.println("\n════════════════════════════════════════");
                System.out.println("           GAME OVER - DAY " + day + "           ");
                System.out.println("════════════════════════════════════════");
                System.out.println(catName + " has been sent to the GULAG! 😿");
                cat.displayStatus();
                break;
            }
            
            // End of day summary
            System.out.println("\n════════════════════════════════════════");
            System.out.println("           END OF DAY " + day + " SUMMARY         ");
            System.out.println("════════════════════════════════════════");
            
            // Check for critical warnings
            boolean allGood = true;
            if (cat.getHappy() <= 30) {
                System.out.println("⚠️  Warning: " + catName + " is getting unhappy!");
                allGood = false;
            }
            if (cat.getHunger() <= 30) {
                System.out.println("⚠️  Warning: " + catName + " needs more food!");
                allGood = false;
            }
            if (cat.getSleep() <= 30) {
                System.out.println("⚠️  Warning: " + catName + " needs more rest!");
                allGood = false;
            }
            if (cat.getBathroom() <= 30) {
                System.out.println("⚠️  Warning: " + catName + "'s litter box needs attention!");
                allGood = false;
            }
            
            if (allGood) {
                System.out.println("✅ All needs met! " + catName + " is content.");
            }
            
            // Ask to continue or quit
            System.out.print("\nPress Enter to continue to next day, or type 'quit' to exit: ");
            String continueInput = scanner.nextLine();
            if (continueInput.equalsIgnoreCase("quit")) {
                System.out.println("\n════════════════════════════════════════");
                System.out.println("     Thanks for playing Cat Lapse!      ");
                System.out.println("════════════════════════════════════════");
                cat.displayStatus();
                break;
            }
            
            day++;
            
            // Optional: Add some variety every few days
            if (day % 3 == 0) {
                System.out.println("\n✨ A new day brings new opportunities! ✨");
            }
        }
        
        // Final game over message if cat survived
        if (!cat.isInGulag()) {
            System.out.println("\n════════════════════════════════════════");
            System.out.println("          CONGRATULATIONS!              ");
            System.out.println("    You kept " + catName + " happy for " + (day-1) + " days!");
            System.out.println("════════════════════════════════════════");
        }
        
        System.out.println("\nGame Over. Thanks for playing!");
        scanner.close();
    }
}