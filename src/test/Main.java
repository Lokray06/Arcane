package test;

import engine.*;
import engine.components.*;
import engine.utils.FileUtils;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

public class Main
{
    public static void main(String[] args)
    {
        Texture red = new Texture(new Vector3i(255, 0, 0));
        Texture green = new Texture(new Vector3i(0, 255, 0));
        Texture blue = new Texture(new Vector4i(0, 0, 255, 20));
        Texture spain = new Texture(FileUtils.load("spain.jpg"));
        Texture prototype = new Texture(FileUtils.load("darkTexture01.png"));
        
        Texture metalAlbedo = new Texture(FileUtils.load("1Albedo.png"));
        Texture metalNormal = new Texture(FileUtils.load("1Normal.png"));
        Texture metalAO = new Texture(FileUtils.load("1AO.png"));
        Material metal = new Material(metalAlbedo, metalNormal, Material.empty.metallicMap, Material.empty.roughnessMap, metalAO);

        CubeMapTexture skyboxTexture = new CubeMapTexture(FileUtils.load("Daylight Box UV.png"));
        Skybox skybox = new Skybox(skyboxTexture);
        
        Material redMaterial = new Material(red);
        Material greenMaterial = new Material(green);
        Material blueMaterial = new Material(blue);
        Material prototypeMaterial = new Material(prototype);

        String meshPath = FileUtils.load("box.obj");
        String meshPath1 = FileUtils.load("suzanne.obj");
        String meshPath2 = FileUtils.load("plane.obj");
        String sphereMeshPath = FileUtils.load("sphere.obj");
        Mesh suzanneMesh = new Mesh(meshPath1);
        Mesh boxMesh = new Mesh(meshPath);
        Mesh floorMesh = new Mesh(meshPath2);
        Mesh sphereMesh = new Mesh(sphereMeshPath);

        Material spainMaterial = new Material(spain);

        GameObject floor = new GameObject("Floor");
        floor.transform.scale = new Vector3f(1).mul(50);
        floor.transform.position = new Vector3f(0, -5, 0);
        floor.addComponent(MeshRenderer.class);
        floor.getComponent(MeshRenderer.class).mesh = floorMesh;
        floor.getComponent(MeshRenderer.class).material = spainMaterial;
        
        GameObject wall = new GameObject("Wall");
        wall.transform.scale = new Vector3f(1).mul(50);
        wall.transform.position = new Vector3f(0, 25, 50);
        wall.transform.rotation = new Vector3f(90, 0, 0);
        wall.addComponent(MeshRenderer.class);
        wall.getComponent(MeshRenderer.class).mesh = floorMesh;

        GameObject sphere = new GameObject("Suzanne", new Transform(new Vector3f(10, 0, 0), new Vector3f(3f)));
        sphere.addComponent(MeshRenderer.class);
        sphere.getComponent(MeshRenderer.class).mesh = sphereMesh;
        sphere.getComponent(MeshRenderer.class).material = metal;
        
        GameObject sphere2 = new GameObject("littleSphere", new Transform(new Vector3f(0, 0, 5)));
        sphere2.addComponent(MeshRenderer.class);
        sphere2.getComponent(MeshRenderer.class).mesh = sphereMesh;
        sphere2.getComponent(MeshRenderer.class).material = redMaterial;

        GameObject box = new GameObject("box", new Transform(new Vector3f(-10, 0, 0)));
        box.addComponent(MeshRenderer.class);
        box.addComponent(SuzanneComponent.class);
        box.getComponent(MeshRenderer.class).mesh = boxMesh;
        box.getComponent(MeshRenderer.class).material = prototypeMaterial;

        GameObject suzanne2 = new GameObject("Suzanne2", new Transform(new Vector3f(10, 0, 10)));
        suzanne2.addComponent(MeshRenderer.class);
        suzanne2.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne2.getComponent(MeshRenderer.class).material = greenMaterial;
        suzanne2.addComponent(SuzanneComponent.class);
        suzanne2.addChild(sphere2);
        box.addChild(suzanne2);

        GameObject suzanne3 = new GameObject("Suzanne3", new Transform(new Vector3f(0, 0, -10), new Vector3f(2f)));
        suzanne3.addComponent(MeshRenderer.class);
        suzanne3.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne3.getComponent(MeshRenderer.class).material = blueMaterial;

        Scene scene = new Scene("TestScene");

        GameObject camera = new GameObject("Camera", new Transform(new Vector3f(0, 0, 0)));
        Camera cameraComponent = new Camera(70);
        camera.addComponent(cameraComponent);
        camera.addComponent(CameraController.class);
        camera.getComponent(Camera.class).isActive = true;
        
        GameObject sun = new GameObject("Sun");
        sun.transform.rotation.x = 90;
        sun.addComponent(LightDirectional.class);
        sun.addComponent(SuzanneComponent.class);
        
        scene.getRootGameObject().addComponent(GameStuff.class);
        scene.getRootGameObject().addComponent(skybox);
        
        scene.addGameObject(sun);
        scene.addGameObject(box);
        scene.addGameObject(sphere);
        scene.addGameObject(sphere2);
        scene.addGameObject(floor);
        scene.addGameObject(wall);
        scene.addGameObject(suzanne2);
        scene.addGameObject(suzanne3);
        scene.addGameObject(camera);
        
        Engine.activeScene = scene;
        Engine.init();
    }
}