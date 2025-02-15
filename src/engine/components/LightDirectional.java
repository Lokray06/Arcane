package engine.components;

import engine.Component;
import org.joml.Vector3f;

public class LightDirectional extends Component
{
    public Vector3f color = new Vector3f(255);
    public float strength = 10;
}
