package snippet;

public class Snippet {
	 for (int row = 0; row < gameBoard.length; row++)
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

