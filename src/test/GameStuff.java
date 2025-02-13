package test;

import engine.Component;
import engine.Input;

public class GameStuff extends Component
{
    public static boolean inGame = false;
    
    @Override
    public void update()
    {
        if(inGame)
        {
            if(Input.getKey("escape"))
            {
                Input.setCursorLocked(false);
                inGame = false;
            }
        }
        else
        {
            if(Input.isMouseClickInBounds(0))
            {
                Input.setCursorLocked(true);
                inGame = true;
            }
        }
    }
}
