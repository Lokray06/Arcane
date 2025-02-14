package test;

import engine.*;
import engine.components.Camera;
import engine.components.MeshRenderer;
import engine.components.Transform;
import engine.utils.FileUtils;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Main
{
    public static void main(String[] args)
    {
        Texture red = new Texture(new Vector3i(255, 0, 0));
        Texture green = new Texture(new Vector3i(0, 255, 0));
        Texture blue = new Texture(new Vector3i(0, 0, 255));
        Texture spain = new Texture(FileUtils.load("spain.jpg"));

        Material redMaterial = new Material(red);
        Material greenMaterial = new Material(green);
        Material blueMaterial = new Material(blue);

        String meshPath = FileUtils.load("box.obj");
        String meshPath1 = FileUtils.load("suzanne.obj");
        Mesh suzanneMesh = new Mesh(meshPath1);
        Mesh boxMesh = new Mesh(meshPath);

        Material spainMaterial = new Material(spain);

        GameObject floor = new GameObject("Floor", new Transform(new Vector3f( 0,-52, 0), new Vector3f(50)));
        floor.addComponent(MeshRenderer.class);
        floor.getComponent(MeshRenderer.class).mesh = boxMesh;
        floor.getComponent(MeshRenderer.class).material = spainMaterial;

        GameObject suzanne = new GameObject("Suzanne", new Transform(new Vector3f(10, 0, 0), new Vector3f(0.1f)));
        suzanne.addComponent(MeshRenderer.class);
        suzanne.addComponent(SuzanneComponent.class);
        suzanne.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne.getComponent(MeshRenderer.class).material = redMaterial;


        GameObject box = new GameObject("box", new Transform(new Vector3f(0, 0, 10)));
        box.addComponent(MeshRenderer.class);
        box.addComponent(SuzanneComponent.class);
        box.getComponent(MeshRenderer.class).mesh = boxMesh;
        //box.addComponent(new Texture(Texture.Type.ALBEDO));

        GameObject suzanne2 = new GameObject("Suzanne2", new Transform(new Vector3f(-10, 0, 0)));
        suzanne2.addComponent(MeshRenderer.class);
        suzanne2.addComponent(SuzanneComponent.class);
        suzanne2.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne2.getComponent(MeshRenderer.class).material = greenMaterial;

        GameObject suzanne3 = new GameObject("Suzanne3", new Transform(new Vector3f(0, 0, -10), new Vector3f(2f)));
        suzanne3.addComponent(MeshRenderer.class);
        suzanne3.addComponent(SuzanneComponent.class);
        suzanne3.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne3.getComponent(MeshRenderer.class).material = blueMaterial;

        Scene scene = new Scene("TestScene");

        GameObject camera = new GameObject("Camera", new Transform(new Vector3f(0, 0, 0)));
        Camera cameraComponent = new Camera(70);
        camera.addComponent(cameraComponent);
        camera.addComponent(CameraController.class);
        camera.getComponent(Camera.class).isActive = true;
        
        scene.getRootGameObject().addComponent(GameStuff.class);
        
        scene.addGameObject(box);
        scene.addGameObject(suzanne);
        scene.addGameObject(floor);
        scene.addGameObject(suzanne2);
        scene.addGameObject(suzanne3);
        scene.addGameObject(camera);
        
        Engine.activeScene = scene;
        Engine.init();
    }
}