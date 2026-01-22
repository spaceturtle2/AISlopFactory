public class VirtualPet
{
  // Instance Variables
  private int energy, happiness, weight, ageInYears, months;
  private String name;
  private boolean sick;
    
  // Constant Variables
  private static final int MINIMUM_WEIGHT = 5;
  private static final int MAXIMUM_LEVEL = 10;
    
  // Constructor
  public VirtualPet(String petName)
  {
    name = petName;
    energy = 0;
    happiness = 0;
    weight = MINIMUM_WEIGHT;
    months = 0; 
    ageInYears = 0;
    sick = false;
  }
    
  // Accessor Method   
  public String getName()
  {
    return name;
  }
     
  //  add feed, getEnergyLevel, and getHappinessLevel methods here

  
  // returns a String of the state of the object
  public String toString()
  {
    return name + "'s Information:\nEnergy: " + energy + "\nHappiness: " 
                    + happiness + "\nWeight: " + weight + " g\nAge: " 
                    + months + " months and " + ageInYears + " years";    
  }
public void feed(Food f) 
{
	if (this.energy < MAXIMUM_LEVEL) {
	this.energy += f.getEnergyIncrease();
	this.weight += f.getWeightGain();
	this.happiness += f.getHapinessIncrease();
	}
}
public int getEnergyLevel() {
	return this.energy;
}
public int getHappinessLevel() {
	return this.happiness;
}


public boolean play(Game g) {
if (this.weight > MINIMUM_WEIGHT && this.happiness < MAXIMUM_LEVEL && g.isWinner() == true) {
	this.happiness += g.getHapinessIncrease();
	this.weight -= g.getWeightDecrease();
}
return g.isWinner();
}

public void updateStatus() {
	if ((int) Math.random() * 100 + 1 > 80) sick = true; else sick = false;
	if (this.happiness > 0) {
		this.happiness -= 1;
	}
	if (this.energy > 0) {
		this.energy -= 1;
	}
	if (months < 11) {
		months += 1;
	}
	else if (months == 11) {
		months = 0;
		ageInYears += 1;
	}
}
public void cure() {
	this.sick = false;
}
public boolean checkcure() {
	if (this.sick == true) {
		return true;
	}
	return false;
}

}
