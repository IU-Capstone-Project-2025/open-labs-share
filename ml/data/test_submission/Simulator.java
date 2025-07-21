import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.Color;

/**
 * A simple predator-prey simulator, based on a rectangular field
 * containing lions, hyenas, porcupines, zebras and buffalos.
 *
 * @version 2019.02.22
 */
public class Simulator
{
    // Constants representing configuration information for the simulation.
    // The default width for the grid.
    private static final int DEFAULT_WIDTH = 240;
    // The default depth of the grid.
    private static final int DEFAULT_DEPTH = 240;
    // The probability that a zebra will be created in any given grid position.
    private static final double ZEBRA_CREATION_PROBABILITY = 0.100;
    // The probability that a lion will be created in any given grid position.
    private static final double LION_CREATION_PROBABILITY = 0.01;  
    // The probability that a hyena will be created in any given grid position.
    private static final double HYENA_CREATION_PROBABILITY = 0.02;
    // The probability that a porcupine will be created in any given grid position.
    private static final double PORCUPINE_CREATION_PROBABILITY = 0.30;
    // The probability that a buffalo will be created in any given grid position.
    private static final double BUFFALO_CREATION_PROBABILITY = 0.40;
    
    private String dayTime;
    // List of animals in the field.
    private List<Animal> animals;
    
    
    private List<Plant> plants;
    // The current state of the field.
    private Field field;
    // The current step of the simulation.
    private int step;
    // A graphical view of the simulation.
    private SimulatorView view;
    private static Weather weather = new Weather();
    
    /**
     * Construct a simulation field with default size.
     */
    public Simulator()
    {
        this(DEFAULT_DEPTH, DEFAULT_WIDTH);
    }
    
    /**
     * Create a simulation field with the given size.
     * @param depth Depth of the field. Must be greater than zero.
     * @param width Width of the field. Must be greater than zero.
     */
    public Simulator(int depth, int width)
    {
        if(width <= 0 || depth <= 0) 
        {
            System.out.println("The dimensions must be greater than zero.");
            System.out.println("Using default values.");
            depth = DEFAULT_DEPTH;
            width = DEFAULT_WIDTH;
        }
        plants = new ArrayList<>();
        animals = new ArrayList<>();
        field = new Field(depth, width);
        field.createPlantField();
        // Create a view of the state of each location in the field.
        view = new SimulatorView(depth, width);
        view.setColor(Zebra.class, Color.BLACK);
        view.setColor(Lion.class, Color.ORANGE);
        view.setColor(Hyena.class, Color.BLUE);
        view.setColor(Porcupine.class, Color.YELLOW);
        view.setColor(Buffalo.class, Color.PINK);
        view.setPlantsColor(Plant.class, Color.GREEN);
        // Setup a valid starting point.
        reset();
    }
    
    /**
     * Run the simulation from its current state for a reasonably long period,
     * (4000 steps).
     */
    public void runLongSimulation()
    {
        simulate(4000);
    }
    
    /**
     * Run the simulation from its current state for the given number of steps.
     * Stop before the given number of steps if it ceases to be viable.
     * @param numSteps The number of steps to run for.
     */
    public void simulate(int numSteps)
    {
        for(int step = 1; step <= numSteps && view.isViable(field); step++) 
        {
            simulateOneStep();
            //delay(1000);   // uncomment this to run more slowly
        }
    }
    
    /**
     * Changes the time of the day from day to night
     */
    public String changeDayTime(String dayTime)
    {
        return this.dayTime = dayTime;
    }
    
    /**
     * Run the simulation from its current state for a single step.
     * Iterate over the whole field updating the state of each
     * fox and rabbit.
     */
    public void simulateOneStep()
    {
        step++; 
        showTime(step);
        changeWeather(step);
        // Provide space for newborn animals.
        List<Animal> newAnimals = new ArrayList<>();
        
        // Let all animals act.
        for(Iterator<Animal> it = animals.iterator(); it.hasNext();) 
        {
            Animal animal = it.next();
            animal.act(step);
            animal.reproduce(newAnimals);
            
            if(!animal.isAlive()) 
            {
                it.remove();
                
            }
        }
               
        // Add the newly born animals to the main lists.
        animals.addAll(newAnimals);
        
        view.showStatus(step, showTime(step), field, weather.returnWeather());
    }
    
    /**
     * Every 5 steps the weather changes and the plants grow or die, according to the weather
     */
    private void changeWeather(int step)
    {
        if(step%5 == 0)
        {
            weather.weatherStep();
            weather.returnWeather();
            growPlants();
            killPlants();
        }
    }
    
    /**
     * Make the plants grow if it's raining
     */
    private void growPlants()
    {
        if(weather.isRaining())
        {
            resetPlants();
        }
    }
    
    /**
     * Make the plants die if it's drought
     */
    private void killPlants()
    {
        if(weather.isDrought())
        {
            for(Iterator<Plant> it = plants.iterator(); it.hasNext();) 
            {
                Plant plant = it.next();
                it.remove();
            }
        }
    }
    
    /**
     * Changes the time of the day according to the hour
     * The hour is defined as the rest from division of steps with 24
     */
    private String showTime(int step)
    {
        if(step%24 >= 7 && step%24 <= 20)
        {
            return changeDayTime("Day: " + step%24 + ":00");
        }
        else
        {
            return changeDayTime("Night: " + step%24 + ":00");
        }
    }
        
    /**
     * Reset the simulation to a starting position.
     */
    public void reset()
    {
        step = 0;
        animals.clear();
        populate();
        plants.clear();
        // Show the starting state in the view.
        view.showStatus(step, showTime(step), field, weather.returnWeather());
    }
    
    /**
     * Randomly populate the field with animals and plants.
     */
    private void populate()
    {
        Random rand = Randomizer.getRandom();
        field.clear();
        for(int row = 0; row < field.getDepth(); row++) 
        {
            for(int col = 0; col < field.getWidth(); col++) 
            {
                if(rand.nextDouble() <= LION_CREATION_PROBABILITY) 
                {
                    Location location = new Location(row, col);
                    Lion lion = new Lion(true, field, location);
                    animals.add(lion);
                }
                else if(rand.nextDouble() <= HYENA_CREATION_PROBABILITY) 
                {
                    Location location = new Location(row, col);
                    Hyena hyena = new Hyena(true, field, location);
                    animals.add(hyena);
                }
                else if(rand.nextDouble() <= ZEBRA_CREATION_PROBABILITY) 
                {
                    Location location = new Location(row, col);
                    Zebra zebra = new Zebra(true, field, location);
                    animals.add(zebra);
                }
                else if(rand.nextDouble() <= PORCUPINE_CREATION_PROBABILITY) 
                {
                    Location location = new Location(row, col);
                    Porcupine porcupine = new Porcupine(true, field, location);
                    animals.add(porcupine);
                }
                else if(rand.nextDouble() <= BUFFALO_CREATION_PROBABILITY) 
                {
                    Location location = new Location(row, col);
                    Buffalo buffalo = new Buffalo(true, field, location);
                    animals.add(buffalo);
                }
                
                Location location = new Location(row, col);
                Plant plant = new Plant(field.getPlantField(), location);
                plants.add(plant);          //add the plants to the field
            }
        }
    }
    
    /**
     * Add the plants to all the empty spots in the field
     */
    private void resetPlants()
    {
        for(int row = 0; row < field.getDepth(); row++) 
        {
            for(int col = 0; col < field.getWidth(); col++) 
            {
               Location location = new Location(row, col);
               if(field.getObjectAt(location) ==null)
                {
                    Plant plant = new Plant(field.getPlantField(), location);
                    plants.add(plant);
                }
            }
        }
    }
    
    /**
     * Pause for a given time.
     * @param millisec  The time to pause for, in milliseconds
     */
    private void delay(int millisec)
    {
        try 
        {
            Thread.sleep(millisec);
        }
        catch (InterruptedException ie) 
        {
            // wake up
        }
    }
}