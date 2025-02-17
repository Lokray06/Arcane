package engine.components;

import engine.Component;
import org.joml.Vector3f;

/**
 * The {@code LightDirectional} component represents a directional light source.
 * <p>
 * Directional lights simulate light coming from an infinitely distant source in a specified direction.
 * </p>
 */
public class LightDirectional extends Component {
    /** The color of the directional light. */
    public Vector3f color = new Vector3f(255);
    /** The strength (intensity) of the directional light. */
    public float strength = 1;
}
