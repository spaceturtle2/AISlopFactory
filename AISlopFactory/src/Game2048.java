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
     int randRow = (int)(Math.random() * boardSize);
     int randCol = (int)(Math.random() * boardSize);
     while (gameBoard[randRow][randCol] != 0)
     {
       randRow = (int)(Math.random() * boardSize);
       randCol = (int)(Math.random() * boardSize);
     }
     gameBoard[randRow][randCol] = 2;
   }     
     
   public void moveLeft()
   { for (int row = 0; row < gameBoard.length; row++)
   {
	      for (int col = 0; col < gameBoard[0].length - 1; col++)
	      {
	         if (gameBoard[row][col] == 0)
	         {
	            int counter = col + 1;
	            while (counter < gameBoard[0].length && gameBoard[row][counter] == 0)
	            {
	               counter++;
	            }

	            if (counter < gameBoard[0].length)
	            {
	               gameBoard[row][col] = gameBoard[row][counter];
	               gameBoard[row][counter] = 0;
	            }
	         }
	      }
	     }
   }
   
   public void moveRight()
   {
	   for (int row = 0; row < gameBoard.length; row++)
	   {
		      for (int col = gameBoard[0].length - 1; col >= 0; col--)
		      {
		         if (gameBoard[row][col] == 0)
		         {
		            int counter = col - 1;
		            while (counter >= 0 && gameBoard[row][counter] == 0)
		            {
		               counter--;
		            }

		            if (counter >= 0)
		            {
		               gameBoard[row][col] = gameBoard[row][counter];
		               gameBoard[row][counter] = 0;
		            }
		         }
		      }
		     }
   }
      
   public void moveUp()
   {
	   for (int col = 0; col < gameBoard.length; col++)
	   {
		      for (int row = 0; row < boardSize; row++)
		      {
		         if (gameBoard[row][col] == 0)
		         {
		            int counter = row + 1;
		            while (counter < boardSize && gameBoard[counter][col] == 0)
		            {
		               counter++;
		            }

		            if (counter < gameBoard[0].length)
		            {
		               gameBoard[row][col] = gameBoard[counter][col];
		               gameBoard[counter][col] = 0;
		            }
		         }
		      }
		     }
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
         for (int col = 0; col < boardSize; col++)
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