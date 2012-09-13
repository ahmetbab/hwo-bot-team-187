package redlynx.pong.state;


public class GameStatus {

    public void extrapolate(double dt) {
        left.y += left.vy * dt; // bind inside play area?
        right.y += right.vy * dt;

        // TODO: Extrapolate wall collisions
        ball.x += ball.vx * dt;
        ball.y += ball.vy * dt;
    }

    public void reset() {
        left.reset();
        right.reset();
        ball.reset();
        conf.reset();
    }

    public Player getPedal(PongGameBot.PlayerSide side) {
        if(side == PongGameBot.PlayerSide.LEFT)
            return left;
        return right;
    }

    public boolean hits(PongGameBot.PlayerSide pedalSide, Ball ball) {

        // if game has not started
        if(ball.y < 0)
            return false;

        Player pedal = getPedal(pedalSide);
        if(pedalSide == PongGameBot.PlayerSide.LEFT) {
            if(ball.x < conf.paddleWidth) {
                boolean notOverTop = ball.y - conf.ballRadius < pedal.y;
                boolean notUnderBot = ball.y > pedal.y - conf.paddleHeight;

                if(notOverTop && notUnderBot) {
                    // calculate relative collision point. (-1 = pedal bottom, +1  pedal top)
                    double bally = ball.y - conf.ballRadius * 0.5;
                    double pedaly = pedal.y - conf.paddleHeight * 0.5;
                    double delta = bally - pedaly;
                    delta /= conf.paddleHeight * 0.5;

                    System.out.println("Hit pedal at " + delta);
                    ball.vx *= -1;
                    ball.x += ball.vx * 0.05;
                    return true;
                }
            }
        }
        else {
            if(ball.x + conf.ballRadius > conf.maxWidth - conf.paddleWidth) {
                boolean notOverTop = ball.y - conf.ballRadius < pedal.y;
                boolean notUnderBot = ball.y > pedal.y - conf.paddleHeight;

                if(notOverTop && notUnderBot) {
                    // calculate relative collision point. (-1 = pedal bottom, +1  pedal top)
                    double bally = ball.y - conf.ballRadius * 0.5;
                    double pedaly = pedal.y - conf.paddleHeight * 0.5;
                    double delta = bally - pedaly;
                    delta /= conf.paddleHeight * 0.5;

                    System.out.println("Hit pedal at " + delta);
                    ball.vx *= -1;
                    ball.x += ball.vx * 0.05;
                    return true;
                }
            }
        }

        return false;
    }

    public static class Player {
		public double y = -1;
        public double vy = 0;
		public String name;

        public void copy(Player player) {
            y = player.y;
            vy = player.vy;
            name = player.name;
        }

        public void reset() {
            y = -1;
            vy = 0;
        }

    }

	public static class Ball {
		public double x = -1;
		public double y = -1;
        public double vx = 0;
        public double vy = 0;

        public void copy(Ball ball) {
            x = ball.x;
            y = ball.y;
            vx = ball.vx;
            vy = ball.vy;
        }

        public void reset() {
            x = y = -1;
            vx = vy = 0;
        }

        public void tick(float dt) {
            x += vx * dt;
            y += vy * dt;
        }

        public String getVelocityString() {
            return "(" + vx + ", " + vy + ")";
        }
    }

	public static class Conf {
		public int maxWidth = -1;
		public int maxHeight = -1;
		public int paddleHeight;
		public int paddleWidth;
		public int ballRadius;
		public int tickInterval;

        public void copy(Conf conf) {
            maxHeight = conf.maxHeight;
            maxWidth = conf.maxWidth;
            paddleHeight = conf.paddleHeight;
            paddleWidth = conf.paddleWidth;
            ballRadius = conf.ballRadius;
            tickInterval = conf.tickInterval;
        }

        public void reset() {
        }
    }

    public GameStatus() {
		left = new Player();
		right = new Player();
		ball = new Ball();
		conf = new Conf();
	}
	
	public long time;
	public final Player left;
	public final Player right;
	public final Ball ball;
	public final Conf conf;

    // This is to be used on consecutive states obtained from servers.
    // If update intervals are long, these results cannot be trusted,
    // and should instead rely on our own physics update.
    public void update(GameStatus gameStatus) {

        // dt is seconds. server time is milliseconds.
        double dt = (gameStatus.time - time) / 1000.0 + 0.00000001; // incase division by zero
        // System.out.println("Server dt: " + dt);
        gameStatus.ball.vx = (gameStatus.ball.x - ball.x) / dt;
        gameStatus.ball.vy = (gameStatus.ball.y - ball.y) / dt;
        gameStatus.left.vy = (gameStatus.left.y - left.y) / dt;
        gameStatus.right.vy = (gameStatus.right.y - right.y) / dt;
    }

    public void copy(GameStatus gameStatus) {
        time = gameStatus.time;
        left.copy(gameStatus.left);
        right.copy(gameStatus.right);
        ball.copy(gameStatus.ball);
        conf.copy(gameStatus.conf);
    }
		
	public String toString() {
		return "time: "+time+"\n"
		+"player1: y: "+left.y+" name "+left.name+"\n"
		+"player2: y: "+right.y+" name "+right.name+"\n"
		+"ball: x: "+ ball.x+" y: "+ ball.y+"\n"
		+"conf: area: ("+conf.maxWidth+", "+conf.maxHeight+") paddle ("+conf.paddleWidth+", "+conf.paddleHeight+") ball radius "+conf.ballRadius+" tickInterval: "+conf.tickInterval+"\n";
	}
	
}
