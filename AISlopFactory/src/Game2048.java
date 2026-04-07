/**
 * Game2048.java  
 *
 * @author – Your name
 * @author – Class period
 *
 */  
public class Game2048
{
   private int[][] gameBoard;
   private int score;
   private int boardSize;   
   
   public Game2048()
   {
      boardSize = 4;
      gameBoard = new int[boardSize][boardSize];
      score = 0;
      add2ToBoard();
      add2ToBoard();
   }
   
   public void add2ToBoard()
   {
	  boolean found = false; 
      while (found == false) {
	  int row = (int)(Math.random()*boardSize);
      int col = (int)(Math.random()*boardSize);
      if (gameBoard[row][col] == 0) {
    	  gameBoard[row][col] = 2;
    	  found = true;
      }
      }
      
   }      
     
   public void moveLeft()
   {
      /* To be completed in Activity 3 Part A */
   }
   
   public void moveRight()
   {
      /* To be completed in Activity 3 Part B */
   }
      
   public void moveUp()
   {
      /* To be completed in Activity 3 Part C */
   }
   
   public void moveDown()
   {
      /* To be completed in Activity 3 Part D */
   }
   
   public void mergeLeft()
   {
      /* To be completed in Activity 4 Part A*/
   }

   public void mergeRight()
   {
      /* To be completed in Activity 4 Part B*/
   }
   
   public void mergeUp()
   {
      /* To be completed in Activity 4 Part C */
   }

   public void mergeDown()
   {
      /* To be completed in Activity 4 Part D */
   }
   
   public boolean gameOver()
   {
      /* To be completed in Activity 5 */
      return false;
   }   
   
   public void displayBoard()
   {
      System.out.println("Score: " + score);
      System.out.println();
      for (int row = 0; row < gameBoard.length; row++)
      {
         for (int col = 0; col < gameBoard[0].length; col++)
         {
            if (gameBoard[row][col] == 0)
            {
               System.out.print("-\t\t");
            } 
            else if (gameBoard[row][col] < 100)
            {
               System.out.print(gameBoard[row][col] + "\t\t");
            }
            else 
            {
               System.out.print(gameBoard[row][col] + "\t");
            }
         }
         System.out.println();
      }
      System.out.println();
   }
               
}