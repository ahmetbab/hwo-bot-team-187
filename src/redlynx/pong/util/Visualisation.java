package redlynx.pong.util;

import redlynx.pong.ui.UILine;

import java.awt.*;
import java.util.ArrayList;

public class Visualisation {
    public static void drawCross(ArrayList<UILine> lines, Color color, double x, double y) {
        if(lines != null) {
            lines.add(new UILine(x - 5, y - 5, x + 5, y + 5, color));
            lines.add(new UILine(x - 5, y + 5, x + 5, y - 5, color));
        }
    }

    public static void drawSquare(ArrayList<UILine> lines, Color color, double x, double y) {
        if(lines != null) {
            lines.add(new UILine(x - 5, y - 5, x - 5, y + 5, color));
            lines.add(new UILine(x - 5, y + 5, x + 5, y + 5, color));
            lines.add(new UILine(x + 5, y + 5, x + 5, y - 5, color));
            lines.add(new UILine(x + 5, y - 5, x - 5, y - 5, color));
        }
    }
}
