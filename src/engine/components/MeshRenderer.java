package engine.components;

import engine.Component;
import engine.Material;
import engine.Mesh;

/**
 * The {@code MeshRenderer} component is responsible for rendering a mesh.
 * <p>
 * It holds a reference to a {@link Mesh} and a {@link Material} that define the geometry and appearance of a game object.
 * </p>
 */
public class MeshRenderer extends Component {
    /** The mesh to be rendered. */
    public Mesh mesh;
    /** The material used to render the mesh. Defaults to an empty material. */
    public Material material = Material.empty;
}
