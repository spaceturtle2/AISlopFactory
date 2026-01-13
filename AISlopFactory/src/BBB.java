import java.util.Scanner;

public class BBB {
	private Worker[] BoxHQ = new Worker [64];
	private int currentWorkers = 0;
	
	public void hire(String t, String n) {
        Worker temp = new Worker(t, n);
        BoxHQ[currentWorkers] = temp;
        currentWorkers++;
    }
	
	public Worker getWorkerByID (int serchId) {
		for (int a = 1; a <= 64; a++) {
			if (BoxHQ[a].getId() == serchId) {
				return BoxHQ[a];
			}
		}
		return null;
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
		if (command.substring(0,3).equals("hire")) {
			company.hire(command.substring(4,5), command.substring(6,command.length()));
			System.out.println(company.temp.getPay());
		}
		if (command.substring(0,2).equals("job")) {
			
		}
		}
	}
}
