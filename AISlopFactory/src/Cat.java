/**
 * Meow Meow Cat Simulator 
 *
 * Mielo & Griffin
 * 
 * Take care of a cat that is secretly a Russian spy You may put it in the Gulag, but it can also put you in the Gulag! 
 */

public class Cat {
    private String name;
    private int happiness; 
    private int hunger;
    private int sleep;
    private int bathroom;
    private boolean inGulag;
    
    private static int wcount = 0; 

    // Hires a worker with a job and name, but also assigns them a worker ID and pay automatically. 
    public Cat(String n) {
    	// Assigns the worker name
        this.name = n;
        this.happiness = 50;
        this.hunger = 70;
        this.sleep = 100;
        this.bathroom = 70;
        this.inGulag = false;
    }

    /*
    Several accessor mains to identify and pull workers, specifically to be used in the final bill.
    */

    
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
    