package engine.components;

import engine.Component;
import org.joml.Vector3f;

public class LightPoint extends Component {
    // The light’s color.
    public Vector3f color = new Vector3f(0,0,255);
    // The base intensity.
    public float strength = 50.0f;
    
    // Attenuation factors.
    public float constant = 1.0f;
    public float linear = 0.09f;
    public float quadratic = 0.032f;
}
