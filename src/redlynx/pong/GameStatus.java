package redlynx.pong;


public class GameStatus {

    public void extrapolate(double dt) {
        left.y += left.vy * dt; // bind inside play area?
        right.y += right.vy * dt;

        // TODO: Extrapolate wall collisions
        ball.x += ball.vx * dt;
        ball.y += ball.vy * dt;
    }

    public static class Player {
		double y = -1;
        double vy = 0;
		String name;

        public void copy(Player player) {
            y = player.y;
            vy = player.vy;
            name = player.name;
        }
	}

	public static class Ball {
		double x = -1;
		double y = -1;
        double vx = 0;
        double vy = 0;

        public void copy(Ball ball) {
            x = ball.x;
            y = ball.y;
            vx = ball.vx;
            vy = ball.vy;
        }
	}

	protected static class Conf {
		int maxWidth = -1;
		int maxHeight = -1;
		int paddleHeight;
		int paddleWidth;
		int ballRadius;
		int tickInterval;

        public void copy(Conf conf) {
            maxHeight = conf.maxHeight;
            maxWidth = conf.maxWidth;
            paddleHeight = conf.paddleHeight;
            paddleWidth = conf.paddleWidth;
            ballRadius = conf.ballRadius;
            tickInterval = conf.tickInterval;
        }
	}

    GameStatus() {
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

        // TODO: Is time measured in milliseconds on the server?
        double dt = (gameStatus.time - time) / 1000.0;
        gameStatus.ball.vx = (gameStatus.ball.x - ball.x) / dt;
        gameStatus.ball.vy = (gameStatus.ball.y - ball.y) / dt;
        gameStatus.left.vy = (gameStatus.left.y - left.y) / dt;
        gameStatus.right.vy = (gameStatus.right.y - right.y) / dt;

        copy(gameStatus);
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
