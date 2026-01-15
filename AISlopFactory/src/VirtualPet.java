public class VirtualPet
{
  // Instance Variables
  private int energy, happiness, weight, ageInYears, months;
  private String name;
    
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
public void feed() 
{
	if (this.energy <= 9) {
	this.energy += 1;
	}
	this.weight += 1;
}
public int getEnergyLevel() {
	return this.energy;
}
public int getHappinessLevel() {
	return this.happiness;
}

}
