import java.util.List;
import java.util.Iterator;
import java.util.Random;

/**
 * A simple model of a lion.
 * Lions age, move, eat prey, and die.
 *
 * @version 2019.02.22
 */
public class Lion extends Predator
{
    // Characteristics shared by all lions (class variables).
    
    // The age at which a lion can start to breed.
    private static final int BREEDING_AGE = 8;
    // The age to which a lion can live.
    private static final int MAX_AGE = 70;
    // The likelihood of a lion breeding.
    private static final double BREEDING_PROBABILITY = 0.10;
    // The maximum number of births.
    private static final int MAX_LITTER_SIZE = 2;
    // The food value of animals. In effect, this is the
    // number of steps a lion can go before it has to eat again.
    private static final int ZEBRA_FOOD_VALUE = 20;
    private static final int PORCUPINE_FOOD_VALUE = 15;
    private static final int BUFFALO_FOOD_VALUE = 35;
    
    private static final int MAX_FOOD_LEVEL = 50;
    
    // A shared random number generator to control breeding.
    private static final Random rand = Randomizer.getRandom();
    
    // Individual characteristics (instance fields).
    // The lion's age.
    private int age;
    // The lion's food level, which is increased by eating rabbits.
    private int foodLevel;

    /**
     * Create a fox. A lion can be created as a new born (age zero
     * and not hungry) or with a random age and food level.
     * 
     * @param randomAge If true, the lion will have random age and hunger level.
     * @param field The field currently occupied.
     * @param location The location within the field.
     */
    public Lion(boolean randomAge, Field field, Location location)
    {
        super(field, location);
        if(randomAge) 
        {
            age = rand.nextInt(MAX_AGE);
            foodLevel = rand.nextInt(BUFFALO_FOOD_VALUE);
        }
        else 
        {
            age = 0;
            foodLevel = ZEBRA_FOOD_VALUE;       //assign the food level of a zebra when it's born
        }
    }
    
    /**
     * This is what the lion does most of the time: it hunts for
     * prey. In the process, it might breed, die of hunger,
     * or die of old age.
     * @param field The field currently occupied.
     * @param newFoxes A list to return newly born foxes.
     */
    public void act(int step)
    {
        incrementAge();
        incrementHunger();
        
        if(isAlive()) 
        {    
            // Move towards a source of food if found.
            Location newLocation = findFood();
            if(newLocation == null) 
            { 
                // No food found - try to move to a free location.
                newLocation = getField().freeAdjacentLocation(getLocation());
            }
            // See if it was possible to move.
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
     * Increase the age. This could result in the lion's death.
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
     * Increase the hunger. This could result in the lion's death.
     */
    private void incrementHunger()
    {
        foodLevel--;
        
        if(foodLevel <= 0) 
        {
            setDead();
        }
    }
    
    /**
     * Checks if the lion has reached it's max food value
     */
    protected boolean isFull()
    {
        boolean full = false;
        if(foodLevel > MAX_FOOD_LEVEL) 
        {
            full = true;
        }
        return full;
    }
    
    /**
     * Increases the food level of a lion everytime it eats a prey
     * If it's full, it stops eating
     */
    protected void addFoodValue(int foodValue)
    {
        foodLevel += foodValue;
        isFull();
    }
    
    /**
     * Return the max food level of a lion
     */
    protected int getMaxFood()
    {
        return MAX_FOOD_LEVEL;
    }
    
    /**
     * Look for prey adjacent to the current location.
     * Only the first live prey is eaten.
     * @return Where food was found, or null if it wasn't.
     */
    private Location findFood()
    {
        Field field = getField();
        List<Location> adjacent = field.adjacentLocations(getLocation());
        Iterator<Location> it = adjacent.iterator();
        
        while(it.hasNext()) 
        {
            Location where = it.next();
            Object animal = field.getObjectAt(where);
            
            if(animal instanceof Zebra) 
            {
                Zebra zebra = (Zebra) animal;
                if(zebra.isAlive()) 
                { 
                    zebra.setDead();
                    foodLevel = ZEBRA_FOOD_VALUE;
                    return where;
                }
            }
            else if(animal instanceof Buffalo) 
            {
                Buffalo buffalo = (Buffalo) animal;
                
                if(buffalo.isAlive()) 
                { 
                    buffalo.setDead();
                    foodLevel = BUFFALO_FOOD_VALUE;
                    return where;
                }
            }
            else if(animal instanceof Porcupine) 
            {
                Porcupine porcupine = (Porcupine) animal;
                
                if(porcupine.isAlive()) 
                { 
                    porcupine.setDead();
                    foodLevel = PORCUPINE_FOOD_VALUE;
                    return where;
                }
            }
        }
        return null;
    }
    
    /**
     * Creates a new instance of a lion
     */
    protected Animal newOffspring(boolean randomAge, Field field, Location location)
    {
      Animal offspring;
      offspring = new Lion(false, field, location);
      return offspring;
    }

    /**
     * A lion can breed if it has reached the breeding age.
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
     * Return the breeding probabaility
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
