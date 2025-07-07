
/**
 * Abstract class Predator - Sorts the predators 
 *
 * @version 2019.02.22
 */
public abstract class Predator extends Animal
{
    /**
     * Constructor for the Prey abstract class
     */
    public Predator(Field field, Location location)
    {
        super(field, location);
    }
    
    /**
     * Method to be passed to the animal class and then to the simulator
     */
    abstract public void act(int step);
}