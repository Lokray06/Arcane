package engine;

public class Time {
    public static double deltaTime = 0.0; // Time between frames in seconds
    public static double fixedDeltaTime = 0.016666666666666666f; // Time for fixed updates (1/60 seconds)
    private static long lastTime = System.nanoTime(); // Last frame time
    private static long lastFixedTime = System.nanoTime(); // Last fixed update time
    
    public static void update() {
        long now = System.nanoTime();
        deltaTime = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;
        
        // Update fixedDeltaTime
        double timeSinceLastFixedUpdate = (now - lastFixedTime) / 1_000_000_000.0;
        fixedDeltaTime = Math.min(timeSinceLastFixedUpdate, fixedDeltaTime); // Prevent fixedDeltaTime from exceeding the fixed update interval
        lastFixedTime = now;
    }
    
    public static void sec() {
        // Reset the counts each second
        deltaTime = 0.0;
        fixedDeltaTime = 1.0 / 60.0; // Reset to 0.01666... seconds (60 FPS)
    }
}
