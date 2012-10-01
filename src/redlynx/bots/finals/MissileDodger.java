package redlynx.bots.finals;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;

import java.util.ArrayList;

public class MissileDodger {

    public static double dodge(PongGameBot bot, double relativeVelocity) {
        ClientGameState.Conf conf = bot.lastKnownStatus.conf;
        ClientGameState.Player player = bot.lastKnownStatus.left;

        if(bot.getAvoidables().size() > 0) {
            double myPos = player.y + 0.5 * conf.paddleHeight;
            ArrayList<Double> velocityScores = new ArrayList<Double>(100);

            for(int i=0; i<100; ++i) {
                velocityScores.add(+1.0);
                for(PongGameBot.Avoidable avoidable : bot.getAvoidables()) {
                    double testVelocity = (i - 50) / 50.0;
                    double dPos = avoidable.t * bot.getPaddleMaxVelocity() * testVelocity;
                    double paddleTop = myPos + dPos + 30 + 0.5 * conf.paddleHeight;
                    double paddleBot = myPos + dPos - 30 - 0.5 * conf.paddleHeight;

                    double inBot = avoidable.y - paddleBot;
                    double inTop = paddleTop - avoidable.y;

                    if(inBot * inTop > 0) {
                        velocityScores.set(i, -1.0);
                    }
                }
            }

            int index = (int) ((relativeVelocity * 0.5 + 0.5) * 99);
            if(velocityScores.get(index) > 0) {
                // all ok, current velocity is fine.
            }
            else {
                for(int i=1; i<100; ++i) {
                    int indexBot = index - i;
                    int indexTop = index + i;

                    if(indexBot >= 0 && indexBot < velocityScores.size()) {
                        if(velocityScores.get(indexBot) > 0) {
                            System.out.println("Fixed paddle velocity.");
                            relativeVelocity = (indexBot - 50) / 50.0;
                            break;
                        }
                    }

                    if(indexTop >= 0 && indexTop < velocityScores.size()) {
                        if(velocityScores.get(indexTop) > 0) {
                            System.out.println("Fixed paddle velocity.");
                            relativeVelocity = (indexTop - 50) / 50.0;
                            break;
                        }
                    }
                }
            }
        }

        return relativeVelocity;
    }
}
