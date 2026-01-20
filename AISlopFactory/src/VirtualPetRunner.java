
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VirtualPetRunner
{
    // Prints out menu and returns user choice
    public static int getChoice(Scanner input)
    {
      int selection = 0;
      while (selection < 1 || selection > 4)
      {
         System.out.println("------Virtual Pet Menu------");
         System.out.println("1. Get Pet Information");
         System.out.println("2. Feed Pet" );
         System.out.println("3. Play with Pet" );
         System.out.println("4. Quit" );
         System.out.print("Enter your choice ..... ");
         selection = input.nextInt();
      }
      return selection;
    }
    
    // Prints out food options and returns user choice
    public static int getPantry(Scanner input)
    {   
      int selection = 0;
      while (selection < 1 || selection > 5)
      {
        System.out.println("------Pantry Inventory------");
        System.out.println("1. Apple");
        System.out.println("2. Cupcake" );
        System.out.println("3. Broccoli" );
        System.out.println("4. Potato");
        System.out.println("5. Return to Menu" );
        selection = input.nextInt();
      }
      return selection;

    }
    
     // Prints out game options and returns user choice
    public static int getGame(Scanner input)
    {
      int selection = 0;
      while (selection < 1 || selection > 4)
      {
        System.out.println("------Game Options------");
        System.out.println("1. Coin Toss");
        System.out.println("2. Hoop Jumping" );
        System.out.println("3. Simon Says" );
        System.out.println("4. Return to Menu" );
        selection = input.nextInt();
      }
      return selection;
    }
    
   // Displays a picture of the pet
    public static void printPet(String emo)
    {
        System.out.println(" /\\_/\\");  
        System.out.println("( o.o )"); 
        System.out.println(" > " + emo + " <");
    }
    
    public static void main(String[] args) 
    {
        // CHANGE THIS VARIABLE VALUE TO TEST AT A DIFFERENT SPEED
        final int INTERVAL_IN_SECONDS = 10;
        
        // Sets up Scanner for user input
        Scanner input = new Scanner(System.in);
        
        VirtualPet myPet = new VirtualPet("Coco");
        
        // Instantiates Food objects
        Food apple = new Food("Apple", 2, 1, 1); 
        Food cupcake = new Food("Cupcake", 1, 2, 2);
        Food broccoli = new Food("Broccoli", 3, -1, 1);
        Food potato = new Food("Potato", 2, 0, 2);
        
        // Instantiates Game objects
        Game coinToss = new Game("Coin Toss", 1, 0);
        Game hoopJumping = new Game("Hoop Jumping", 2, 2);
        Game simonSays = new Game("Simon Says", 1, 2);
        
        // Sets up a ScheduledExecutorService object that will call updateStatus
        // every 1 minute.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> { myPet.updateStatus(); }, 0, INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        
        System.out.println(myPet);
        printPet("ᵔ");

        int choice = getChoice(input);        
        while (choice != 4)
        {
            if(choice == 1)
            {
              System.out.println(myPet);
            }
            else if (choice == 2)
            {
                System.out.println("Select a food option from the pantry.");
                int food = getPantry(input);
                Food f = null;
                if (food == 1)
                  f = apple;
                else if (food == 2)
                  f = cupcake;
                else if (food == 3)
                  f = broccoli;
                else if (food == 4)
                  f = potato;
                if (f != null)
                {
                  myPet.feed(f);
                  System.out.println("\n\nYou have fed " + myPet.getName() 
                                        + " 1 " + f.getName());
                }
            }
            else if (choice == 3)
            {
                System.out.println("Select a game to play.");
                int game = getGame(input);
                Game g = null;
                if (game == 1)
                  g = coinToss;
                else if (game == 2)
                  g = hoopJumping;
                else if (game == 3)
                  g = simonSays;
                if (g != null)
                {
                  boolean hasWon = myPet.play(g);
                  System.out.println("\n\nYou have played " + g.getName() 
                                        + " with " + myPet.getName());
                  if (hasWon)
                     System.out.println(myPet.getName() + " has won!");
                  else
                     System.out.println(myPet.getName() + " has lost!");
                }
            }
            if (myPet.getEnergyLevel() >= 5  && myPet.getHappinessLevel() >= 5)
                printPet("ᵕ");
            else
                printPet("ᵔ");
            System.out.println(myPet.getName().toUpperCase());
            choice = getChoice(input);
        }
        scheduler.shutdown();
    }
}