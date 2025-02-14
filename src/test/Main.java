package test;

import engine.*;
import engine.components.Camera;
import engine.components.MeshRenderer;
import engine.components.Transform;
import engine.utils.FileUtils;
import org.joml.Vector3f;

import java.io.File;

public class Main
{
    public static void main(String[] args)
    {
        String meshPath = FileUtils.load("box.obj");
        String meshPath1 = FileUtils.load("suzanne.obj");
        Mesh suzanneMesh = new Mesh(meshPath1);
        Mesh boxMesh = new Mesh(meshPath);

        Material spainMaterial = new Material(new Texture(FileUtils.load("spain.jpg")));

        GameObject floor = new GameObject("Floor", new Transform(new Vector3f( 0,-52, 0), new Vector3f(50)));
        floor.addComponent(MeshRenderer.class);
        floor.getComponent(MeshRenderer.class).mesh = boxMesh;

        GameObject suzanne = new GameObject("Suzanne", new Transform(new Vector3f(10, 0, 0), new Vector3f(0.1f)));
        suzanne.addComponent(MeshRenderer.class);
        suzanne.addComponent(SuzanneComponent.class);
        suzanne.getComponent(MeshRenderer.class).mesh = suzanneMesh;


        GameObject box = new GameObject("box", new Transform(new Vector3f(0, 0, 10)));
        box.addComponent(MeshRenderer.class);
        box.addComponent(SuzanneComponent.class);
        box.getComponent(MeshRenderer.class).mesh = boxMesh;
        //box.addComponent(new Texture(Texture.Type.ALBEDO));

        suzanne.getComponent(MeshRenderer.class).material = spainMaterial;
        GameObject suzanne2 = new GameObject("Suzanne2", new Transform(new Vector3f(-10, 0, 0)));
        suzanne2.addComponent(MeshRenderer.class);
        suzanne2.addComponent(SuzanneComponent.class);
        suzanne2.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        
        GameObject suzanne3 = new GameObject("Suzanne3", new Transform(new Vector3f(0, 0, -10), new Vector3f(2f)));
        suzanne3.addComponent(MeshRenderer.class);
        suzanne3.addComponent(SuzanneComponent.class);
        suzanne3.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        
        Scene scene = new Scene("TestScene");
        
        GameObject camera = new GameObject("Camera", new Transform(new Vector3f(0, 0, 0)));
        Camera cameraComponent = new Camera(70);
        camera.addComponent(cameraComponent);
        camera.addComponent(CameraController.class);
        camera.getComponent(Camera.class).isActive = true;
        
        scene.getRootGameObject().addComponent(GameStuff.class);
        
        scene.addGameObject(suzanne);
        scene.addGameObject(box);
        scene.addGameObject(floor);
        scene.addGameObject(suzanne2);
        scene.addGameObject(suzanne3);
        scene.addGameObject(camera);
        
        Engine.activeScene = scene;
        Engine.init();
    }
}