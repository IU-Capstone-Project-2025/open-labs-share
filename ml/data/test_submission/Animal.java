import java.util.List;
import java.util.Random;
import java.util.Iterator;
/**
 * A class representing shared characteristics of animals.
 *
 * @version 2016.02.29 (2)
 */
public abstract class Animal
{
    // Whether the animal is alive or not.
    private boolean alive;
    // The animal's field.
    private Field field;
    // The animal's position in the field.
    private Location location;
    
    private int gender;
    protected int foodLevel;
    
    private Random rand = new Random();
    
    
    /**
     * Create a new animal at location in field.
     * 
     * @param field The field currently occupied.
     * @param location The location within the field.
     */
    public Animal(Field field, Location location)
    {
        alive = true;
        this.field = field;
        setLocation(location);
        gender = rand.nextInt(2);
    }
    
    /**
     * Make this animal act - that is: make it do
     * whatever it wants/needs to do.
     * @param newAnimals A list to receive newly born animals.
     */
    abstract public void act(int step);

    /**
     * Check whether the animal is alive or not.
     * @return true if the animal is still alive.
     */
    protected boolean isAlive()
    {
        return alive;
    }
    
    protected void reproduce(List<Animal> createdAnimals)
    {
        if(isAlive()){
     Field field = getField();
     List<Location> adjacentLoc = field.adjacentLocations(getLocation());
     Iterator <Location> it = adjacentLoc.iterator();
     while(it.hasNext())
     {
         Location place = it.next();
         Object animal = field.getObjectAt(place);
         Animal secondAnimal = (Animal) animal;
         if(secondAnimal != null && this.getClass().equals(secondAnimal.getClass())){
         if(this.isMale()&& !secondAnimal.isMale()|| !this.isMale()&& secondAnimal.isMale())
         {
            List<Location> free = field.getFreeAdjacentLocations(getLocation());
            int births = breed();
            for(int b = 0; b < births && free.size() > 0; b++) 
                {
                    Location loc = free.remove(0);
                    Animal young = newOffspring(false, field, loc);
                    createdAnimals.add(young);
                }
         }
        }
     }
    }
    }
    
    /**
     * Generate a number representing the number of births,
     * if it can breed.
     * @return The number of births (may be zero).
     */
    private int breed()
    {
        int births = 0;
        if(canBreed() && rand.nextDouble() <= getBreedingProb()) 
        {
            births = rand.nextInt(getLitterSize()) + 1;
        }
        return births;
    }
    
    protected abstract double getBreedingProb();
    
    protected abstract int getLitterSize();
    
    protected abstract int getBreedingAge();
    
    protected abstract int getAge();
    
    protected abstract int getMaxFood();

    protected abstract Animal newOffspring(boolean randomAge, Field field, Location location);
    
    protected boolean maxFood()
    {
        boolean maxFood = false;
        
        if( foodLevel > getMaxFood() ) 
        {
            maxFood = true;
        }
        
        return maxFood;
    }
    
    protected void addFoodValue(int foodValue)
    {
        foodLevel += foodValue;
        maxFood();
    }
    
    private boolean isMale()
    {
        if(gender == 1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * A fox can breed if it has reached the breeding age.
     */
    private boolean canBreed()
    {
        return getAge() >= getBreedingAge();
    }

    /**
     * Indicate that the animal is no longer alive.
     * It is removed from the field.
     */
    protected void setDead()
    {
        alive = false;
        if(location != null) 
        {
            field.clear(location);
            location = null;
            field = null;
        }
    }

    /**
     * Return the animal's location.
     * @return The animal's location.
     */
    protected Location getLocation()
    {
        return location;
    }
    
    /**
     * Place the animal at the new location in the given field.
     * @param newLocation The animal's new location.
     */
    protected void setLocation(Location newLocation)
    {
        if(location != null) 
        {
            field.clear(location);
        }
        
        location = newLocation;
        field.place(this, newLocation);
    }
    
    /**
     * Return the animal's field.
     * @return The animal's field.
     */
    protected Field getField()
    {
        return field;
    }
}
