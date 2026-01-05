import java.util.Scanner;
import java.util.Random;

public class BBB {
	private static Random generator = new Random();
	private Worker[] BoxHQ = new Worker [64];
	private static int currentWorkers = 0;
	
	public void hire(String t, String n) {
        Worker temp = new Worker(t, n);
        BoxHQ[currentWorkers] = temp;
        currentWorkers++;
    }
	
	public Worker getWorkerByID (int serchId) {
		for (int a = 0; a <= 64; a++) {
			if (BoxHQ[a].getId() == serchId) {
				return BoxHQ[a];
			}
		}
		return null;
	}
	
	public Worker abduction() {
		if (currentWorkers == 0) {
			System.out.println("Not enough workers");
			System.exit(0);
		}
		else if (currentWorkers == 1) {
			return getWorkerByID(0);
		}
		int randomGuy = generator.nextInt(currentWorkers - 1);
		return getWorkerByID(randomGuy);
	}
	
	public double pay (Worker w, int hours) {
		
		if (hours <= 40) {
			return w.getPay() * hours;
		}
		else {
			return (w.getPay() * 0.5 * (hours - 40)) + (w.getPay() * hours);
		}
	}
	
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		BBB company = new BBB();
		
		while(true) {
		System.out.print(">>> ");
		String command = scan.nextLine();
		System.out.println(command);
		
		if (command.substring(0,4).equals("hire")) { 
		    company.hire(command.substring(5,6), command.substring(7));
		    int lastHiredId = currentWorkers - 1; 
		    Worker justHired = company.getWorkerByID(lastHiredId);
		    
		    if (justHired != null) {
		        System.out.println("Hired " + justHired.getName() + " with pay: $" + justHired.getPay());
		    }
		}
		
		if (command.substring(0,3).equals("job")) {
			Worker Employee = company.abduction();
			String workerName = Employee.getName();
			double price = Employee.getPay();
			int hoursWorked = 0;
			String jobTitle;
			// jobs: s = copy pase skyscraper, r = gaslight someone into thinking builds are not boxes, c = crash out 
			// copy paste takes 6 hours, gaslighting someone takes 41 hours, and a crash out takes 1 hour
			String jobType = command.substring(4,5);
			System.out.println(jobType);
			if (jobType.equals("s")) {
				price = price * 6;
				jobTitle = "Succesfully copy pasted a skyscraper!";
				hoursWorked = 6;
			}
			else if (jobType.equals("r")) {
				price = price * 41;
				price += (Employee.getPay()*0.5);
				jobTitle = "Succesfully gaslight someone into believing that the boxes are not boxes!";
				hoursWorked = 41;
			}
			else {
				jobTitle = "Destroyed all of spawn!";
				hoursWorked = 1;
			}
			System.out.println("\n");
			System.out.println("Bill!");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println(jobTitle);
			System.out.println("Worker: " + workerName);
			System.out.println("Time spent: " + hoursWorked + " hours");
			System.out.println("You only pay this worker $" + Employee.getPay() + " an hour!");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println("Subtotal = $" + price);
			if (hoursWorked == 41) {
				System.out.println("Don't worry, they got some overtime pay.");
			}
			System.out.println("Tax = $100 (Because I feel like it)");
			System.out.println("Total = $" + (price + 100));
			System.out.println(".");
			System.out.println("Thank you for choosing Ben's Big Box something something... go away.");
			System.out.println("\n");
		}
		if (command.substring(0,4).equals("menu")) {
			System.out.println("\n");
			System.out.println("Menu!");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println("Workers:");
			System.out.println("m = Master baiter ($20 / hour)");
			System.out.println("s = Copy Paster ($15 / hour)");
			System.out.println("r = Ragebaiter ($1 / hour)");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println("Jobs:");
			System.out.println("s = Copy paste skyscraper (6 hours)");
			System.out.println("r = Give friendly talks to people (41 hours)");
			System.out.println("c = Secret (1 hour I think)");
			System.out.println(".");
			System.out.println(".");
			System.out.println(".");
			System.out.println("Commands:");
			System.out.println("hire (job) (name)");
			System.out.println("job (type)");
			System.out.println("\n");	
			
		}
		
		}
	}
}
