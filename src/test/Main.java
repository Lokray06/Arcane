package test;

import engine.*;
import engine.components.*;
import engine.meshTypes.MeshOBJ;
import engine.utils.FileUtils;
import engine.utils.Skybox;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

public class Main
{
    public static void main(String[] args) throws IllegalAccessException
    {
        Texture red = new Texture(new Vector3i(255, 0, 0));
        Texture green = new Texture(new Vector3i(0, 255, 0));
        Texture blue = new Texture(new Vector4i(0, 0, 255, 20));
        Texture spain = new Texture(FileUtils.load("spain.jpg"));
        Texture prototype = new Texture(FileUtils.load("darkTexture01.png"));
        
        Texture metallicTest = new Texture(FileUtils.load("metallicTest.png"));
        
        //Panels
        Texture panelsAlbedo = new Texture(FileUtils.load("worn-modern-panels_albedo.png"));
        Texture panelsAO = new Texture(FileUtils.load("worn-modern-panels_ao.png"));
        Texture panelsMetallic = new Texture(FileUtils.load("worn-modern-panels_metallic.png"));
        Texture panelsNormal = new Texture(FileUtils.load("worn-modern-panels_normal-ogl.png"));
        Texture panelsRoughness = new Texture(FileUtils.load("worn-modern-panels_roughness.png"));
        Texture panelsHeight = new Texture(FileUtils.load("worn-modern-panels_height.png"));
        Material panels = new Material(panelsAlbedo, panelsNormal, panelsMetallic, panelsRoughness, panelsAO, panelsHeight, 1, 1);
        panels.name = "Panels";
        
        //Metal
        Texture metalAlbedo = new Texture(FileUtils.load("1Albedo.png"));
        Texture metalNormal = new Texture(FileUtils.load("1Normal.png"));
        Texture metalAO = new Texture(FileUtils.load("1AO.png"));
        Material metal = new Material(metalAlbedo, metalNormal, Material.empty.metallicMap, Material.empty.roughnessMap, metalAO, 1, 0);

        //Bricks
        Texture bricksAlbedo = new Texture(FileUtils.load("redbricks2b-albedo.png"));
        Texture bricksNormal = new Texture(FileUtils.load("redbricks2b-normal.png"));
        Texture bricksAO = new Texture(FileUtils.load("redbricks2b-ao.png"));
        Texture bricksMetallic = new Texture(FileUtils.load("redbricks2b-metalness.png"));
        Texture bricksHeight = new Texture(FileUtils.load("redbricks2b-height4b.png"));
        Texture bricksRoughness = new Texture(FileUtils.load("redbricks2b-rough.png"));
        Material bricks = new Material(bricksAlbedo, bricksNormal, bricksMetallic, bricksRoughness, bricksAO, bricksHeight, 1, 1);
        
        //CubeMapTexture skyboxTexture = new CubeMapTexture(FileUtils.load("brown_photostudio_02_4k.png"));
        //CubeMapTexture skyboxTexture = new CubeMapTexture(FileUtils.load("Parking-Garage-Müller-Roth-4K.hdr"));
        CubeMapTexture skyboxTexture = new CubeMapTexture(FileUtils.load("milkyWay.jpg"));
        //CubeMapTexture skyboxTexture = new CubeMapTexture(FileUtils.load("Daylight Box UV.png"), true);
        Skybox skybox = new Skybox(skyboxTexture);
        
        Material redMaterial = new Material(red, 0, 1);
        Material greenMaterial = new Material(green, 1, 0);
        Material blueMaterial = new Material(blue, 0, 0);
        Material prototypeMaterial = new Material(prototype, 1, 1);

        String meshPath = FileUtils.load("box.obj");
        String meshPath1 = FileUtils.load("suzanne.obj");
        String meshPath2 = FileUtils.load("plane.obj");
        String sphereMeshPath = FileUtils.load("sphere.obj");
        Mesh suzanneMesh = new MeshOBJ(meshPath1);
        Mesh boxMesh = new MeshOBJ(meshPath);
        Mesh floorMesh = new MeshOBJ(meshPath2);
        Mesh sphereMesh = new MeshOBJ(sphereMeshPath);

        Material spainMaterial = new Material(spain);

        GameObject floor = new GameObject("Floor");
        floor.transform.scale = new Vector3f(1).mul(50);
        floor.transform.position = new Vector3f(0, -5, 0);
        floor.addComponent(MeshRenderer.class);
        floor.getComponent(MeshRenderer.class).mesh = floorMesh;
        floor.getComponent(MeshRenderer.class).material = spainMaterial;
        
        GameObject wall = new GameObject("Wall");
        wall.transform.scale = new Vector3f(50, 50, 1);
        wall.transform.position = new Vector3f(0, 25, -50);
        wall.addComponent(MeshRenderer.class);
        wall.getComponent(MeshRenderer.class).mesh = boxMesh;
        wall.getComponent(MeshRenderer.class).material = prototypeMaterial;

        GameObject sphere = new GameObject("Sphere", new Transform(new Vector3f(10, 0, 0), new Vector3f(3f)));
        sphere.addComponent(MeshRenderer.class);
        sphere.getComponent(MeshRenderer.class).mesh = sphereMesh;
        sphere.getComponent(MeshRenderer.class).material = panels;
        
        GameObject sphere3 = new GameObject("Sphere3", new Transform(new Vector3f(-10, 0, -4), new Vector3f(1f)));
        sphere3.addComponent(MeshRenderer.class);
        sphere3.getComponent(MeshRenderer.class).mesh = sphereMesh;
        sphere3.getComponent(MeshRenderer.class).material = metal;
        
        GameObject sphere2 = new GameObject("sphere2(light)", new Transform(new Vector3f(0, 2, 0), new Vector3f(0.0f)));
        sphere2.addComponent(MeshRenderer.class);
        sphere2.getComponent(MeshRenderer.class).mesh = sphereMesh;
        sphere2.getComponent(MeshRenderer.class).material = Material.empty;

        GameObject box = new GameObject("box", new Transform(new Vector3f(-10, 0, 0)));
        box.addComponent(MeshRenderer.class);
        box.addComponent(SuzanneComponent.class);
        box.getComponent(MeshRenderer.class).mesh = boxMesh;
        box.getComponent(MeshRenderer.class).material = bricks;

        GameObject suzanne2 = new GameObject("Suzanne2", new Transform(new Vector3f(10, 0, 10)));
        suzanne2.addComponent(MeshRenderer.class);
        suzanne2.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne2.getComponent(MeshRenderer.class).material = greenMaterial;
        sphere2.addComponent(LightPoint.class);
        box.addChild(suzanne2);
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
        camera.addChild(sphere2);
        
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
        scene.addGameObject(sphere3);
        scene.addGameObject(floor);
        scene.addGameObject(wall);
        scene.addGameObject(suzanne2);
        scene.addGameObject(suzanne3);
        scene.addGameObject(camera);
        
        Engine.activeScene = scene;
        Engine.init();
    }
}