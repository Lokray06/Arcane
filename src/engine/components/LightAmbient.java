package engine.components;

import engine.Component;
import org.joml.Vector3f;

public class LightAmbient extends Component
{
    private Vector3f color = new Vector3f(255);
    private int intensity = 1;
}
