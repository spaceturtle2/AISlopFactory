public class Worker {
    private String name;
    private String type;
    private double pay;
    private int id;
    
    private static int wcount = 0; 

    public Worker(String t, String n) {
        this.name = n;
        
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
        
        this.id = wcount;
        wcount++;
    }

    public String getName() {
        return this.name;
    }
    
    public int getId() {
        return this.id;
    }
}