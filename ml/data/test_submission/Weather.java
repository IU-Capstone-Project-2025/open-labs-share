import java.util.Random;

/**
 * Provide the weather which will influence the growth of the plants
 *
 * @version 2019.02.22
 */
public class Weather
{
    //The chance to rain
    private static double RAIN_CHANCE = 0.7;
    //The chance to be drought
    private static double DROUGHT_CHANCE = 0.2;         
    
    private boolean rain;
    private boolean sun;
    private boolean drought;
    
    private static final Random rand = Randomizer.getRandom();
    
    /**
     * The constructor for the Weather class. Does nothing.
     */
    public Weather()
    {
    }
    
    /**
     * Method that changes the weather randomly
     */
    public void weatherStep()
    {
        if(rand.nextDouble() <= RAIN_CHANCE)
        {
            drought = false;
            sun = false;
            rain = true;
        }
        else if(rand.nextDouble() <= DROUGHT_CHANCE)
        {
            rain = false;
            sun = false;
            drought = true;
        }
        else
        {
            drought = false;
            rain = false;
            sun = true;
        }
    }
    
    /**
     * Returns the weather as a String
     */
    public String returnWeather()
    {
        if(isRaining() == true)
        {
            return "Raining";
        }
        else if(isDrought() == true)
        {
            return "Drought";
        }
        else
        {
            return "Sunny";
        }
    }
    
    /**
     * Return true if there is Drought
     */
    public boolean isDrought()
    {
          return drought;
    }
    
    /**
     * Return true if there is Sunny
     */
    public boolean isSunny()
    {
          return sun;
    }
    
    /**
     * Return true if there is Raining
     */
    public boolean isRaining()
    {
          return rain;
    }
}