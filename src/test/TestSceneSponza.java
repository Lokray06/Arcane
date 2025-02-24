package test;

import engine.*;
import engine.components.*;
import engine.meshTypes.MeshGLTF;
import engine.meshTypes.MeshOBJ;
import engine.utils.FileUtils;
import engine.utils.Skybox;
import org.joml.Vector3f;
import org.joml.Vector4i;

public class TestSceneSponza
{
    public static void main(String[] args) throws IllegalAccessException
    {
        Texture blue = new Texture(new Vector4i(0, 0, 255, 20));
        String suzanneMeshPath = FileUtils.load("suzanne.obj");
        String sphereMeshPath = FileUtils.load("sphere.obj");
        Material blueMaterial = new Material(blue, 0, 0);
        Material metalMaterial = Material.empty;
        Mesh sponzaMesh = new MeshGLTF(FileUtils.load("Sponza.gltf"));
        Mesh sphereMesh = new MeshOBJ(sphereMeshPath);
        Mesh suzanneMesh = new MeshOBJ(suzanneMeshPath);
        Mesh bottleMesh = new MeshGLTF(FileUtils.load("WaterBottle.gltf"));
        
        GameObject suzanne = new GameObject("Suzanne");
        suzanne.addComponent(MeshRenderer.class);
        suzanne.getComponent(MeshRenderer.class).mesh = suzanneMesh;
        suzanne.getComponent(MeshRenderer.class).material = blueMaterial;
        suzanne.transform.scale = new Vector3f(0.1f);
        suzanne.transform.position = new Vector3f(-1, 1, 0);
        
        GameObject metalSphere = new GameObject("Metal sphere");
        metalSphere.transform.scale = new Vector3f(0.5f);
        metalSphere.addComponent(MeshRenderer.class);
        metalSphere.getComponent(MeshRenderer.class).mesh = sphereMesh;
        metalSphere.getComponent(MeshRenderer.class).material = metalMaterial;
        
        GameObject bottle = new GameObject("Bottle");
        bottle.addComponent(MeshRenderer.class);
        bottle.getComponent(MeshRenderer.class).mesh = bottleMesh;
        bottle.transform.position = new Vector3f(1, 1, 0);
        
        GameObject sponza = new GameObject("Sponza", new Transform(new Vector3f(0), new Vector3f(0.005f)));
        sponza.addComponent(MeshRenderer.class);
        sponza.getComponent(MeshRenderer.class).mesh = sponzaMesh;
        
        Skybox skybox = new Skybox(new CubeMapTexture(FileUtils.load("default4k.png")));

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
        
        //scene.addGameObject(sponza);
        scene.addGameObject(suzanne);
        scene.addGameObject(bottle);
        scene.addGameObject(metalSphere);
        
        scene.addGameObject(sun);
        scene.addGameObject(camera);
        
        Engine.activeScene = scene;
        Engine.init();
    }
}