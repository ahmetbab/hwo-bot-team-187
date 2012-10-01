package redlynx.pong.util;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.ui.UILine;

import java.awt.*;
import java.util.ArrayList;

public class Visualisation {

    public static void visualizeOpponentReach(ArrayList<UILine> lines, PongGameBot bot, double timeLeft) {
        // visualise enemy reach (ignoring missiles)
        double maxDeltaPos = timeLeft * bot.getPaddleMaxVelocity() + 0.5 * bot.lastKnownStatus.conf.paddleHeight;
        double currentPos = bot.lastKnownStatus.right.y + 0.5 * bot.lastKnownStatus.conf.paddleHeight;
        Visualisation.drawVerticalLine(lines, bot.lastKnownStatus.conf.maxWidth - 10, currentPos + maxDeltaPos, currentPos - maxDeltaPos);

        // visualise enemy reach (taking current missiles into account)
        for(PongGameBot.Avoidable missile : bot.getOffensiveMissiles()) {
        }

        // visualise enemy reach (imagining that i fired a missile right now)
        // ..?
    }

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
    public static void drawVector(ArrayList<UILine> lines, Color color, double x, double y, double vx,double vy) {
        if(lines != null) {
            lines.add(UILine.createFromDirection(new Vector2(x,y), new Vector2(vx,vy), 1, color));
        }
    }

    public static void drawVerticalLine(ArrayList<UILine> lines, double x, double y1, double y2) {
        if(lines != null) {
            lines.add(new UILine(x, y1, x, y2, Color.cyan));
        }
    }
}
