import java.util.List;
import java.util.Random;
import java.util.Iterator;
/**
 * A simple model of a zebra.
 * Zebras age, move, breed, and die.
 *
 * @version 2019.02.22
 */
public class Zebra extends Prey
{
    // Characteristics shared by all zebras (class variables).

    // The age at which a zebra can start to breed.
    private static final int BREEDING_AGE = 20;
    // The age to which a zebra can live.
    private static final int MAX_AGE = 100;
    // The likelihood of a zebra breeding.
    private static final double BREEDING_PROBABILITY = 0.10;
    // The maximum number of births.
    private static final int MAX_LITTER_SIZE = 2;
    // A shared random number generator to control breeding.
    private static final Random rand = Randomizer.getRandom();
    
    private static final int MAX_FOOD_LEVEL = 40;
    
    private int foodLevel;
    // Individual characteristics (instance fields).
    
    // The zebra's age.
    private int age;

    /**
     * Create a new zebra. A zebra may be created with age
     * zero (a new born) or with a random age.
     * 
     * @param randomAge If true, the zebra will have a random age.
     * @param field The field currently occupied.
     * @param location The location within the field.
     */
    public Zebra(boolean randomAge, Field field, Location location)
    {
        super(field, location);
        age = 0;
        
        if(randomAge) 
        {
            age = rand.nextInt(MAX_AGE);
            foodLevel = rand.nextInt(30);
        }
        else 
        {
            age = 0;
            foodLevel = 30;
        }
    }
    
    /**
     * This is what the zebra does most of the time - it runs 
     * around. Sometimes it will breed or die of old age.
     */
    public void act(int step)
    {
        incrementAge();

        if(isAlive()) 
        {  
                if(step%24 >= 7 && step%24 <= 20)
                {
                    // Try to move into a free location.
                Location newLocation = getField().freeAdjacentLocation(getLocation());
                if(newLocation != null) 
                {
                    setLocation(newLocation);
                }
                else 
                {
                    // Overcrowding.
                    setDead();
                }
        }
    }
        
    }
    
    /**
     * Increase the age. This could result in the zebra's death.
     */
    private void incrementAge()
    {
        age++;
        
        if(age > MAX_AGE) 
        {
            setDead();
        }
    }
    
    /**
     * Make this zebra more hungry. This could result in the zebra's death.
     */
    private void incrementHunger()
    {
        foodLevel--;
        
        if(foodLevel <= 0) 
        {
            setDead();
        }
    }
    
    protected boolean isFull()
    {
        boolean full = false;
        if( foodLevel > MAX_FOOD_LEVEL) 
        {
            full = true;
        }
        return full;
    }
    
    protected int getMaxFood()
    {
        return MAX_FOOD_LEVEL;
    }
    
    protected Animal newOffspring(boolean randomAge, Field field, Location location)
    {
        Animal young;
        young = new Zebra(false, field, location);
        return young;
    }
    
    /**
     * A fox can breed if it has reached the breeding age.
     */
    private boolean canBreed()
    {
        return age >= BREEDING_AGE;
    }
    
    protected double getBreedingProb()
    {
        return BREEDING_PROBABILITY;
    }
    
    protected int getBreedingAge()
    {
        return BREEDING_AGE;
    }
    
    protected int getLitterSize()
    {
        return MAX_LITTER_SIZE;
    }
    
    protected int getAge()
    {
        return age;
    }
    
}
