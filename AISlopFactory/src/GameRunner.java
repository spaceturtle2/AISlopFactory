/**
 * GameRunner.java  
 *
 * @author – Your name
 * @author – Class period 
 * 
 */ 
import java.util.Scanner; 
public class GameRunner
{
   public static void main(String[] args)
   {
      Game2048 myGame = new Game2048();
      Scanner kb = new Scanner(System.in);
      String userChoice = "";
      
      while (!userChoice.equals("q"))
      {
         System.out.println();
         myGame.displayBoard();
         System.out.println("l - left, r - right, u - up, d - down, q - quit");
         System.out.print("Enter your choice: ");
         userChoice = kb.nextLine();
         while (userChoice.length() == 0)
         {  
           System.out.println("Please enter something ");
           userChoice = kb.nextLine();
         }
         userChoice = userChoice.substring(0,1).toLowerCase();
         if (userChoice.equals("l"))
         {
            myGame.moveLeft();
            myGame.mergeLeft();
            myGame.moveLeft();
         } 
         else if (userChoice.equals("r")) 
         {
            myGame.moveRight();
            myGame.mergeRight();
            myGame.moveRight();
         } 
         else if (userChoice.equals("u")) 
         {
            myGame.moveUp();
            myGame.mergeUp();
            myGame.moveUp();
         } 
         else if (userChoice.equals("d")) 
         {
            myGame.moveDown();
            myGame.mergeDown();
            myGame.moveDown();
         } 
         else 
         {
            System.out.println("Invalid Choice!");
            continue; // goes directly to top of loop
         }
         myGame.add2ToBoard();
      }
      myGame.displayBoard();   
   }
}