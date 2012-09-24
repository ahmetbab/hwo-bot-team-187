package redlynx.pong.util;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.ui.UILine;
import java.awt.Color;

import java.util.ArrayList;

public class PongUtil {

    public static double simulate(ClientGameState.Ball ball, ClientGameState.Conf conf) {
        return simulateNew(ball, conf, null, null);
    }

    public static double simulateNew(ClientGameState.Ball ball, ClientGameState.Conf conf, ArrayList<UILine> lines, Color color) {
        double vy = ball.vy;
        double vx = ball.vx;
        double x = ball.x;
        double y = ball.y;

        if(vx * vx < 0.00001f)
            return 1000000;

        double xLength = 0;
        double end_x = 0;
        if(vx > 0) {
            double maxBallPosX = conf.maxWidth - conf.paddleWidth - conf.ballRadius;
            xLength = maxBallPosX - x;
            end_x = maxBallPosX;
        }
        else {
            xLength = x - conf.paddleWidth - conf.ballRadius;
            end_x = conf.paddleWidth + conf.ballRadius;
        }

        double dy = vy / Math.abs(vx);
        double time = xLength / Math.abs(vx);
        y += dy * xLength;

        double maxBallY = conf.maxHeight - conf.ballRadius;
        while(y < conf.ballRadius || y > maxBallY) {
            vy *= -1;
            if(y < conf.ballRadius) {
                y = 2 * conf.ballRadius - y;
            }
            else {
                y = 2 * maxBallY - y;
            }
        }

        ball.vx = vx;
        ball.vy = vy;
        ball.y = y;
        ball.x = end_x;

        Visualisation.drawSquare(lines, color, ball.x, ball.y);

        return time;
    }

    public static double simulateOld(ClientGameState.Ball ball, ClientGameState.Conf conf, ArrayList<UILine> lines, Color color) {
        double vy = ball.vy;
        double vx = ball.vx;
        double x = ball.x;
        double y = ball.y;

        double last_x = x;
        double last_y = y;

        double totalTime = 0;
        if(vx * vx < 0.00001f)
            return 1000000;

        double dt = 0.005;
        while(x > conf.ballRadius + conf.paddleWidth && x < conf.maxWidth - conf.ballRadius - conf.paddleWidth) {
            x += vx * dt;
            y += vy * dt;

            // if collides with walls, mirror y velocity
            if(y > conf.maxHeight - conf.ballRadius) {
                vy *= -1;
                y = conf.maxHeight - conf.ballRadius;

                if(lines != null)
                    lines.add(new UILine(new Vector2i(last_x, last_y), new Vector2i(x, y), color));

                last_x = x;
                last_y = y;
            }

            if(y < conf.ballRadius) {
                vy *= -1;
                y = conf.ballRadius;

                if(lines != null)
                    lines.add(new UILine(new Vector2i(last_x, last_y), new Vector2i(x, y), color));

                last_x = x;
                last_y = y;
            }

            totalTime += dt;
        }

        if(lines != null)
            lines.add(new UILine(new Vector2i(last_x, last_y), new Vector2i(x, y), color));

        ball.vy = vy;
        ball.vx = vx;
        ball.x = x;
        ball.y = y;

        return totalTime;
    }


    public static double pointDistance2Line(Vector2 a, Vector2 b, Vector2 p) {

        double l2 = a.distanceSquared(b);

        if (l2 == 0.0) {
            return p.distanceSquared(a);
        }

        Vector2 tmp1 = new Vector2(p.x - a.x, p.y - a.y);
        Vector2 tmp2 = new Vector2(b.x - a.x, b.y - a.y);
        double t = (tmp1.x * tmp2.x + tmp1.y * tmp2.y) / l2;

        Vector2 projection = new Vector2(a.x + tmp2.x * t, a.y + tmp2.y * t);
        return p.distanceSquared(projection);
    }



}
