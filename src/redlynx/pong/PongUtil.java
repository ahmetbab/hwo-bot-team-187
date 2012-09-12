package redlynx.pong;

public class PongUtil {

    public static void simulate(GameStatus.Ball ball, GameStatus.Conf conf) {
        double vy = ball.vy;
        double vx = ball.vx;
        double x = ball.x;
        double y = ball.y;

        if(vx * vx < 0.00001f)
            return;

        while(x > conf.ballRadius + conf.paddleWidth && x < conf.maxWidth - conf.ballRadius - conf.paddleWidth) {
            x += vx * 0.05;
            y += vy * 0.05;

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
