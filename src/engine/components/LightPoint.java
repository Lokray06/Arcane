package engine.components;

import engine.Component;
import org.joml.Vector3f;

/**
 * The {@code LightPoint} component represents a point light source.
 * <p>
 * A point light emits light in all directions from a specific position. Attenuation factors control
 * how the light's intensity decreases over distance.
 * </p>
 */
public class LightPoint extends Component {
    /** The color of the point light. */
    public Vector3f color = new Vector3f(0, 0, 255);
    /** The base intensity of the point light. */
    public float strength = 50.0f;
    
    // Attenuation factors:
    /** Constant attenuation factor. */
    public float constant = 1.0f;
    /** Linear attenuation factor. */
    public float linear = 0.09f;
    /** Quadratic attenuation factor. */
    public float quadratic = 0.032f;
}
