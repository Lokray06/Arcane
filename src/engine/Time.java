package engine;

/**
 * The {@code Time} class provides static utilities to track the time elapsed
 * between frames and fixed update intervals.
 * <p>
 * It maintains the delta time between frames as well as a fixed delta time for fixed updates.
 * </p>
 */
public class Time {
    /** Time between frames in seconds. */
    public static double deltaTime = 0.0;
    /**
     * Time for fixed updates (default is 1/60 seconds).
     * This value is adjusted each update to prevent exceeding the fixed update interval.
     */
    public static double fixedDeltaTime = 0.016666666666666666f;
    /** The timestamp (in nanoseconds) of the last frame. */
    private static long lastTime = System.nanoTime();
    /** The timestamp (in nanoseconds) of the last fixed update. */
    private static long lastFixedTime = System.nanoTime();
    
    /**
     * Updates the time measurements.
     * <p>
     * This method calculates the delta time (in seconds) since the last frame and updates the fixed delta time,
     * ensuring it does not exceed the predetermined fixed update interval.
     * </p>
     */
    public static void update() {
        long now = System.nanoTime();
        deltaTime = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;
        
        // Update fixedDeltaTime based on the time elapsed since the last fixed update.
        double timeSinceLastFixedUpdate = (now - lastFixedTime) / 1_000_000_000.0;
        fixedDeltaTime = Math.min(timeSinceLastFixedUpdate, fixedDeltaTime); // Prevent exceeding the fixed update interval.
        lastFixedTime = now;
    }
    
    /**
     * Resets the time counters.
     * <p>
     * This method resets the {@code deltaTime} to zero and restores the {@code fixedDeltaTime} to its default value (1/60 seconds).
     * </p>
     */
    public static void sec() {
        // Reset the counts each second.
        deltaTime = 0.0;
        fixedDeltaTime = 1.0 / 60.0; // Reset to approximately 0.01666 seconds (60 FPS)
    }
}
