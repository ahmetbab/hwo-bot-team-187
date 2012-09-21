package redlynx.pong.util;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.ui.UILine;
import java.awt.Color;

import java.util.ArrayList;

public class PongUtil {

    public static double simulate(ClientGameState.Ball ball, ClientGameState.Conf conf) {
        return simulate(ball, conf, null, null);
    }

    public static double simulate(ClientGameState.Ball ball, ClientGameState.Conf conf, ArrayList<UILine> lines, Color color) {
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
