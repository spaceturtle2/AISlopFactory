public class Food
{
	private String name;
    private int energyIncrease;
    private int happinessIncrease;
    private int weightGain;
    

    public Food(String n, int e, int h, int w) {

        this.name = n;
        this.energyIncrease = e;
        this.happinessIncrease = h;
        this.weightGain = w;
    }

    public String getName() {
        return this.name;
    }
    
    public int getEnergyIncrease() {
        return this.energyIncrease;
    }
    public int getHapinessIncrease() {
    	return this.happinessIncrease;
    }
    public int getWeightGain() {
    	return this.weightGain;
    }
}
