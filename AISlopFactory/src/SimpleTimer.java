public class SimpleTimer {
    private long startTime;

    /**
     * Constructs a SimpleTimer and starts it immediately.
     */
    public SimpleTimer() {
        start();
    }

    /**
     * Resets the timer to the current time.
     */
    public void start() {
        this.startTime = System.nanoTime();
    }

    /**
     * Calculates the time passed (in milliseconds) since the last call to start() 
     * or the constructor. It also resets the start time to the current time.
     *
     * @return The elapsed time in milliseconds.
     */
    public long elapsedAndReset() {
        long currentTime = System.nanoTime();
        // Calculate difference and convert from nanoseconds to milliseconds
        long elapsedTimeMs = (currentTime - this.startTime) / 1_000_000;
        // Reset the start time for the next measurement
        this.startTime = currentTime; 
        return elapsedTimeMs;
    }
}
