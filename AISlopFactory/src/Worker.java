/**
 *  Ben's Big Box Corp. LLC 
 *
 * Mielo & Griffin
 * 
 * A class that contains the information of all workers, including their name, job, pay, and ID. 
 */

public class Worker {
    private String name;
    private String type;
    private double pay;
    private int id;
    
    private static int wcount = 0; 

    // Hires a worker with a job and name, but also assigns them a worker ID and pay automatically. 
    public Worker(String t, String n) {
    	// Assigns the worker name
        this.name = n;
        
        // The three different jobs, each with different pays.
        if (t.equals("m")) {
            this.type = "Master baiter"; 
            this.pay = 20;
        }
        else if (t.equals("s")) {
            this.type = "Copy Paster";
            this.pay = 15;
        }
        else {
            this.type = "Ragebaiter";
            this.pay = 1;
        }
        
        // Assigns their account 
        this.id = wcount;
        wcount++;
    }

    /*
    Several accessor mains to identify and pull workers, specifically to be used in the final bill.
    */

    
    public String getName() {
        return this.name;
    }
    
    public int getId() {
        return this.id;
    }
}