package test;

import engine.Component;
import engine.Input;
import imgui.ImGui; // Import ImGui to check for mouse capture

import static engine.Engine.editor;

public class GameStuff extends Component {
    public static boolean inGame = false;
    
    @Override
    public void update() {
        if (Input.getKeyDown("f1")) {
            Input.setMouseDeltaX(0);
            Input.setMouseDeltaY(0);
            editor.toggle();
            inGame = !inGame;
            Input.setCursorLocked(!Input.isCursorLocked());
        }
//        if (inGame) {
//            if (Input.getKey("escape")) {
//            }
//        } else {
//            // Check if left mouse button is clicked, within game bounds, and not over an ImGui window.
//            if (Input.isMouseClickInBounds(0) && !ImGui.getIO().getWantCaptureMouse()) {
//                Input.setCursorLocked(true);
//                inGame = true;
//            }
//        }
    }
}
