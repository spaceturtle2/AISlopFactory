public class Game
{
	private String name;
    private int happinessIncr;
    private int weightDecr;
    

    public Game(String name, int happinessIncr, int weightDecr) {

        this.name = name;
        this.happinessIncr = happinessIncr;
        this.weightDecr = weightDecr;
    }

    public String getName() {
        return this.name;
    }
    
    public int getHapinessIncrease() {
    	return this.happinessIncr;
    }
    public int getWeightDecrease() {
    	return this.weightDecr;
    }
    
    public boolean isWinner() {
    	double rand = Math.random();
    	if (rand < .5) {
    		return false;
    	}
    	return true;
    	}
}