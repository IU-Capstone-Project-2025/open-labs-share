
/**
 * Abstract class Prey - Sorts the animals who are prey
 *
 * @version 2019.02.22
 */
public abstract class Prey extends Animal
{
    /**
     * Constructor for the Prey abstract class
     */
    public Prey(Field field, Location location)
    {
        super(field, location);
    }
    
    /**
     * Method to be passed to the animal class and then to the simulator
     */
    abstract public void act(int step);
}
