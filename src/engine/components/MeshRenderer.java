package engine.components;

import engine.Component;
import engine.Material;
import engine.Mesh;

public class MeshRenderer extends Component
{
    public Mesh mesh;
    public Material material = new Material();
}
