package test;

import engine.Component;
import engine.Input;
import org.joml.Vector3f;

public class CameraController extends Component
{
    @Override
    public void fixedUpdate()
    {
//        gameObject.transform.rotation.y += 0.01f;
        if(Input.getKey("space"))
        {
            gameObject.transform.position.y += 0.1f;
        }
        if(Input.getKey("shift"))
        {
            gameObject.transform.position.y -= 0.1f;
        }
        if(Input.getKeyDown("w"))
        {
            gameObject.transform.position.add(gameObject.transform.getForward().mul(2, new Vector3f()));
        }
        System.out.println(gameObject.transform);
    }
}
