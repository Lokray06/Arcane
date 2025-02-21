package engine.components;

import engine.Component;
import org.joml.Vector3f;

/**
 * The {@code LightAmbient} component represents an ambient light source.
 * <p>
 * Ambient light affects all objects equally without directionality. This component holds
 * a color and intensity that can be used to modulate scene-wide ambient lighting.
 * </p>
 */
public class LightAmbient extends Component {
    /** The color of the ambient light. */
    private Vector3f color = new Vector3f(255);
    /** The intensity of the ambient light. */
    private int intensity = 1;
}
