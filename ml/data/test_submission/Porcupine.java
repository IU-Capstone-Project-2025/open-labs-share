import java.util.List;
import java.util.Random;

/**
 * A simple model of a porcupine.
 * porcupines age, move, breed, and die.
 *
 * @version 2019.02.22
 */
public class Porcupine extends Prey
{
    // Characteristics shared by all porcupines (class variables).

    // The age at which a porcupine can start to breed.
    private static final int BREEDING_AGE = 8;
    // The age to which a porcupine can live.
    private static final int MAX_AGE = 80;
    // The likelihood of a porcupine breeding.
    private static final double BREEDING_PROBABILITY = 0.15;
    // The maximum number of births.
    private static final int MAX_LITTER_SIZE = 2;
    // A shared random number generator to control breeding.
    private static final Random rand = Randomizer.getRandom();
    
    private static final int MAX_FOOD_LEVEL = 10;
    
    // Individual characteristics (instance fields).
    
    // The porcupine's age.
    private int age;

    /**
     * Create a new porcupine. A porcupine may be created with age
     * zero (a new born) or with a random age.
     * 
     * @param randomAge If true, the porcupine will have a random age.
     * @param field The field currently occupied.
     * @param location The location within the field.
     */
    public Porcupine(boolean randomAge, Field field, Location location)
    {
        super(field, location);
        age = 0;
        
        if(randomAge) 
        {
            age = rand.nextInt(MAX_AGE);
        }
    }
    
    /**
     * This is what the porcupine does most of the time - it runs 
     * around. Sometimes it will breed or die of old age.
     * @param newporcupines A list to return newly born porcupines.
     */
    public void act(int step)
    {
        incrementAge();
        
        if(isAlive()) 
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
    
    /**
     * Creates a new instance of a porcupine
     */
    protected Animal newOffspring(boolean randomAge, Field field, Location location)
    {
        Animal young;
        young = new Porcupine(false, field, location);
        return young;
    }
    
    /**
     * Return the max food level of a porcupine
     */
    protected int getMaxFood()
    {
        return MAX_FOOD_LEVEL;
    }

    /**
     * Increase the age.
     * This could result in the porcupine's death.
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
     * A fox can breed if it has reached the breeding age.
     */
    private boolean canBreed()
    {
        return age >= BREEDING_AGE;
    }
    
    /**
     * Return the breeding probabaility
     */
    protected double getBreedingProb()
    {
        return BREEDING_PROBABILITY;
    }
    
    /**
     * Return the breeding age
     */
    protected int getBreedingAge()
    {
        return BREEDING_AGE;
    }
    
    /**
     * Return the litter size
     */
    protected int getLitterSize()
    {
        return MAX_LITTER_SIZE;
    }
    
    /**
     * Return the age
     */
    protected int getAge()
    {
        return age;
    }
}
