package redlynx.gametree;

import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.collisionmodel.LinearModel;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;

public class AlphaBeta {

    private static PongModel model = new LinearModel();

    public static class AlphaBetaState {
        public double left_y = 0;
        public double right_y = 0;
        public ClientGameState.Ball ball = new ClientGameState.Ball();

        public double getPaddle(boolean leftPlayer) {
            if(leftPlayer)
                return right_y;
            return left_y;
        }
    }

    private static final int MIN_VAL = 3;
    private static final int MAX_VAL = 26;
    private static final double MID_VAL = 15.0;

    public static double alphabeta(AlphaBetaState state, ClientGameState.Conf conf, int depth, double alpha, double beta, boolean leftPlayer, double allowedTime, double maxPaddleSpeed) {

        if(depth == 0) {
            return 0;
        }

        ClientGameState.Ball[] balls = new ClientGameState.Ball[MAX_VAL];
        double[] times = new double[MAX_VAL];

        double min_y = 1000000;
        double max_y = 0;

        for(int i=MIN_VAL; i<MAX_VAL; ++i) {

            balls[i] = new ClientGameState.Ball();
            double deflectVal = (i - MID_VAL) / MID_VAL;
            double inSpeed = state.ball.getSpeed();
            Vector2 out = model.guess(deflectVal, state.ball.vx, state.ball.vy);
            out.normalize().scaled(inSpeed + 15);

            balls[i].copy(state.ball, true);
            balls[i].vx = out.x;
            balls[i].vy = out.y;
            times[i] = PongUtil.simulate(balls[i], conf);

            if(balls[i].y < min_y)
                min_y = balls[i].y;
            if(balls[i].y > max_y)
                max_y = balls[i].y;
        }

        double opponentTarget = min_y + (max_y - min_y) * 0.5;
        double opponentPos = state.getPaddle(leftPlayer);
        double opponentReach = maxPaddleSpeed * allowedTime;

        double deltaSqr = opponentTarget - opponentPos;
        deltaSqr *= deltaSqr;

        // there exists a choice which grants me (kind of) a win. opponent has not enough time to get to best defensive pos.
        if(deltaSqr > opponentReach * opponentReach)
            return deltaSqr - opponentReach * opponentReach;

        AlphaBetaState childState = new AlphaBetaState();
        for(int i=3; i<26; ++i) {
            childState.ball.copy(balls[i], true);
            childState.right_y = opponentTarget;

            double score = -alphabeta(childState, conf, depth-1, -beta, -alpha, !leftPlayer, times[i], maxPaddleSpeed);

            if(score >= beta)
                return beta;

            if(score > alpha)
                alpha = score;
        }

        return alpha;
    }


}
