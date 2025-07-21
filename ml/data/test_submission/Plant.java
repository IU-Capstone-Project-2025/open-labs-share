import java.util.List;
/**
 * Creates and set plants that should be used as a source of food 
 * for the prey. 
 *
 * @version 2019.02.22
 */
public class Plant
{
    private static final int PLANTS_FOOD_VALUE = 20;
    private boolean alive;
    private Location location;
    private Field field;
    
    /**
     * Constructor for objects of class Plant
     */
    public Plant(Field field, Location location)
    {
        alive = true;
        this.field = field;
        this.location = location;
    }

    /**
     * Check if a plant is alive.
     * Returns true if it is alive.
     */
    public boolean isPlantAlive()
    {
        return alive;
    }
    
    /**
     * Set the plant as dead and remove it
     */
    private void setPlantDead()
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
     * Returns the location of a plant
     */
    private Location getLocation()
    {
        return location;
    }
    
    /**
     * Returns the field of the plants
     */
    private Field getField()
    {
        return field;
    }
    

    

}