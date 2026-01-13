/**
 * Meow Meow Cat Simulator 
 *
 * Mielo & Griffin
 * 
 * Take care of a cat that is secretly a Russian spy. If you let it get too unhappy, it will put you in the Gulag! 
 */

public class Cat {
    private String name;
    private int happiness; 
    private int hunger;
    private int sleep;
    private int bathroom;
    
    public Cat(String n) {
        this.name = n;
        this.happiness = 50;
        this.hunger = 70;
        this.sleep = 100;
        this.bathroom = 70;
    }

    /*
    Changing 'em cat stats.
    */

    // When being pet, cared for, or fed.
    public void increaseHappiness(int amount) {
        this.happiness = Math.min(100, this.happiness + amount);
    }
    
    // When the owner does something bad, or if stats are not fulfilled (See down)
    public void decreaseHappiness(int amount) {
        this.happiness = Math.max(0, this.happiness - amount);
    }
    
    // Regens hunger and also depletes restroom usage.
    public void feed(int amount) {
        this.hunger = Math.min(100, this.hunger + (2* amount));
        this.bathroom = Math.max(0, this.bathroom - ( amount));
    }
    
    // Regens rest
    public void rest(int amount) {
        this.sleep = Math.min(100, this.sleep + amount);
    }
    
    // Regens need to use bathroom
    public void useBathroom(int amount) {
        this.bathroom = Math.min(100, this.bathroom + amount);
    }
 
    // Called when x amount of time happens, naturally depletes stats. 
    public int timePassed(int amount) {
    	this.sleep = this.sleep - (1 * amount);
    	this.hunger = this.hunger - (3 * amount);
    	if (this.hunger <= 20 || this.sleep <= 20 || this.bathroom <= 10) {
    		this.happiness = this.happiness - 20;
    	}
    	return this.happiness;
    }
    
    
// Not needed, but use when needed
//--------------------------------------------
    
    public String getName() {
        return this.name;
    }
    public int getHappy() {
    	return this.happiness;
    }
    public int getHunger() {
    	return this.hunger;
    }
    public int getSleep() {
    	return this.sleep;
    }
    public int getBathroom() {
    	return this.bathroom;
    }
}
    