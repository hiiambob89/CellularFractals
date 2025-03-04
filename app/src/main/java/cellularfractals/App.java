/*
 * This source file was generated by the Gradle 'init' task
 */
package cellularfractals;

import cellularfractals.engine.GameLoop;
import cellularfractals.engine.World;

public class App {
    public static void main(String[] args) {
        // Create single World instance
        World world = new World(100, 100, 10);

        // Create game loop with existing world
        GameLoop gameLoop = new GameLoop(world);
        gameLoop.run();
    }
}
