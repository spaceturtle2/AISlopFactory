import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VirtualPetRunner {
    
    // Prints out menu and returns user choice
    public static int getChoice(Scanner input) {
        int selection = 0;
        while (selection < 1 || selection > 4) {
            System.out.println("\n------SINGULARITY MENU------");
            System.out.println("1. Scan Event Horizon (Info)");
            System.out.println("2. Feed the Void (Pantry)");
            System.out.println("3. Distort Space-Time (Play)");
            System.out.println("4. Escape Orbit (Quit)");
            System.out.print("Enter command ..... ");
            if(input.hasNextInt()) {
                selection = input.nextInt();
            } else {
                input.next();
            }
        }
        return selection;
    }

    // Prints out food options and returns user choice
    public static int getPantry(Scanner input) {
        int selection = 0;
        while (selection < 1 || selection > 6) {
            System.out.println("------MATTER INVENTORY------");
            System.out.println("1. Apple");
            System.out.println("2. Cupcake");
            System.out.println("3. Broccoli");
            System.out.println("4. Potato");
            System.out.println("5. Dark Matter Meds");
            System.out.println("6. Return to Menu");
            if(input.hasNextInt()) {
                selection = input.nextInt();
            } else {
                input.next();
            }
        }
        return selection;
    }

    public static int getGame(Scanner input) {
        int selection = 0;
        while (selection < 1 || selection > 4) {
            System.out.println("------WARP OPTIONS------");
            System.out.println("1. Coin Toss");
            System.out.println("2. Hoop Jumping");
            System.out.println("3. Simon Says");
            System.out.println("4. Return to Menu");
            if(input.hasNextInt()) {
                selection = input.nextInt();
            } else {
                input.next();
            }
        }
        return selection;
    }

    // THIS IS THE NEW BLACK HOLE ART
    public static void printPet(String emo) {
        System.out.println("       .  .       ");
        System.out.println("      |\\_@@@_/|    ");
        System.out.println("    @@@@@@@@@@@@@  ");
        System.out.println("   @@@@ " + emo + " @ " + emo + " @@@@ ");
        System.out.println("   @@@@@@ V @@@@@  ");
        System.out.println("    @@@@@@@@@@@@@  ");
        System.out.println("      '--@@@--'    ");
        System.out.println("  [ MASSIVE OBJECT ]");
    }

    public static void main(String[] args) {
        final int INTERVAL_IN_SECONDS = 10;
        Scanner input = new Scanner(System.in);
        VirtualPet myPet = new VirtualPet("Coco");

        // Note: I adjusted Meds to have positive values so the "weight" increases
        Food apple = new Food("Apple", 2, 1, 1);
        Food cupcake = new Food("Cupcake", 1, 2, 2);
        Food broccoli = new Food("Broccoli", 3, -1, 1);
        Food potato = new Food("Potato", 2, 0, 2);
        Food meds = new Food("Medicine", 1, 1, 5); // Increased weight for meds

        Game coinToss = new Game("Coin Toss", 1, 0);
        Game hoopJumping = new Game("Hoop Jumping", 2, 2);
        Game simonSays = new Game("Simon Says", 1, 2);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            myPet.updateStatus();
        }, 0, INTERVAL_IN_SECONDS, TimeUnit.SECONDS);

        System.out.println(myPet);
        printPet("O");

        int choice = getChoice(input);
        while (choice != 4) {
            // Status Warnings
            if (myPet.getHappinessLevel() <= 0) {
                System.out.println("\n[!] The void is unstable and sad.");
            }
            if (myPet.checkcure()) {
                System.out.println("[!] Radiation leak! Pet is sick.");
            }
            if (myPet.getEnergyLevel() <= 0) {
                System.out.println("[!] Singularity is collapsing (eepy).");
            }

            if (choice == 1) {
                System.out.println(myPet);
            } 
            else if (choice == 2) {
                int foodChoice = getPantry(input);
                Food f = null;
                if (foodChoice == 1) f = apple;
                else if (foodChoice == 2) f = cupcake;
                else if (foodChoice == 3) f = broccoli;
                else if (foodChoice == 4) f = potato;
                else if (foodChoice == 5) {
                    f = meds;
                    myPet.cure();
                }

                if (f != null) {
                    myPet.feed(f);
                    System.out.println("\n" + myPet.getName() + " sucked " + f.getName() + " into its core!");
                }
            } 
            else if (choice == 3) {
                int gameChoice = getGame(input);
                Game g = null;
                if (gameChoice == 1) g = coinToss;
                else if (gameChoice == 2) g = hoopJumping;
                else if (gameChoice == 3) g = simonSays;

                if (g != null) {
                    boolean hasWon = myPet.play(g);
                    System.out.println("\nPlaying " + g.getName() + " with the anomaly.");
                    if (hasWon) System.out.println(myPet.getName() + " won and grew heavier!");
                    else System.out.println(myPet.getName() + " lost matter to space.");
                }
            }

            // Visual Logic: Changes eyes based on energy/happiness
            if (myPet.getEnergyLevel() >= 5 && myPet.getHappinessLevel() >= 5) {
                printPet("0"); // Stable heavy core
            } else {
                printPet("o"); // Fading core
            }

            System.out.println("NAME: " + myPet.getName().toUpperCase());
            choice = getChoice(input);
        }

        System.out.println("Closing connection to the singularity...");
        scheduler.shutdown();
        System.exit(0);
    }
}