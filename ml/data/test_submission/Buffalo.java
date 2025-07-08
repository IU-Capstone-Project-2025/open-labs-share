
import java.util.List;
import java.util.Random;

/**
 * A simple model of a rabbit.
 * Rabbits age, move, breed, and die.
 *
 * @version 2016.02.29 (2)
 */
public class Buffalo extends Prey
{
    // Characteristics shared by all rabbits (class variables).

    // The age at which a rabbit can start to breed.
    private static final int BREEDING_AGE = 20;
    // The age to which a rabbit can live.
    private static final int MAX_AGE = 200;
    // The likelihood of a rabbit breeding.
    private static final double BREEDING_PROBABILITY = 0.10;
    // The maximum number of births.
    private static final int MAX_LITTER_SIZE = 2;
    // A shared random number generator to control breeding.
    private static final Random rand = Randomizer.getRandom();
    
    private static final int MAX_FOOD_LEVEL = 50;
    // Individual characteristics (instance fields).
    
    // The rabbit's age.
    private int age;

    /**
     * Create a new rabbit. A rabbit may be created with age
     * zero (a new born) or with a random age.
     * 
     * @param randomAge If true, the rabbit will have a random age.
     * @param field The field currently occupied.
     * @param location The location within the field.
     */
    public Buffalo(boolean randomAge, Field field, Location location)
    {
        super(field, location);
        age = 0;
        
        if(randomAge) 
        {
            age = rand.nextInt(MAX_AGE);
        }
    }
    
    /**
     * This is what the rabbit does most of the time - it runs 
     * around. Sometimes it will breed or die of old age.
     * @param newRabbits A list to return newly born rabbits.
     */
    public void act(int step)
    {
        incrementAge();
        
        if(isAlive()) 
        { 
            if(step%24 >= 7 && step%24 <= 20){
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
    protected Animal newOffspring(boolean randomAge, Field field, Location location)
    {
        Animal young;
        young = new Buffalo(false, field, location);
        return young;
    }

    /**
     * Increase the age.
     * This could result in the rabbit's death.
     */
    private void incrementAge()
    {
        age++;
        
        if(age > MAX_AGE) 
        {
            setDead();
        }
    }
    
    protected int getMaxFood()
    {
        return MAX_FOOD_LEVEL;
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
