package engine;

import engine.components.Transform;

/**
 * The abstract Component class serves as the base for all components
 * that can be attached to GameObjects.
 * <p>
 * Components have lifecycle methods (start, fixedUpdate, and update) that
 * can be overridden to implement specific behavior.
 * </p>
 */
public abstract class Component {
    /** The GameObject this component is attached to. */
    public GameObject gameObject;
    /** The Transform of the GameObject, used for position, rotation, and scale. */
    public Transform transform;
    
    /**
     * Associates this component with a GameObject and initializes its transform.
     *
     * @param gameObject the GameObject to attach this component to.
     */
    public void setGameObject(GameObject gameObject) {
        this.gameObject = gameObject;
        this.transform = gameObject.transform;
    }
    
    /**
     * Called when the component is added to a GameObject.
     * <p>
     * Override this method to perform any necessary initialization.
     * </p>
     */
    public void start() {
        // Called when the component is added to a GameObject.
    }
    
    /**
     * Called at fixed time intervals, typically for physics updates.
     * <p>
     * Override this method to implement fixed timestep behavior.
     * </p>
     */
    public void fixedUpdate() {
        // Called every fixed amount of time.
    }
    
    /**
     * Called every frame to update the component's state.
     * <p>
     * Override this method to implement per-frame behavior.
     * </p>
     */
    public void update() {
        // Called every frame.
    }
}
