package redlynx.pong.util;

import redlynx.pong.client.state.GameStatus;

public class PongUtil {

    public static void simulate(GameStatus.Ball ball, GameStatus.Conf conf) {
        double vy = ball.vy;
        double vx = ball.vx;
        double x = ball.x;
        double y = ball.y;

        if(vx * vx < 0.00001f)
            return;

        double dt = 0.001;
        while(x > conf.ballRadius + conf.paddleWidth && x < conf.maxWidth - conf.ballRadius - conf.paddleWidth) {
            x += vx * dt;
            y += vy * dt;

            // if collides with walls, mirror y velocity
            if(y + conf.ballRadius >= conf.maxHeight) {
                vy *= -1;
            }

            if(y - conf.ballRadius <= 0) {
                vy *= -1;
            }
        }

        ball.vy = vy;
        ball.vx = vx;
        ball.x = x;
        ball.y = y;
    }
}
