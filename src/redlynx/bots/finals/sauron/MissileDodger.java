package redlynx.bots.finals.sauron;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.ui.UILine;
import redlynx.pong.util.Visualisation;
import java.awt.Color;

import java.util.ArrayList;

public class MissileDodger {

    public static double dodge(ArrayList<UILine> lines, PongGameBot bot, double relativeVelocity) {
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
                    double paddleTop = myPos + dPos + 15 + 0.5 * conf.paddleHeight;
                    double paddleBot = myPos + dPos - 15 - 0.5 * conf.paddleHeight;
                    boolean possiblePosition = paddleBot > 0 && paddleTop < conf.maxHeight;

                    if( (avoidable.y < paddleTop && avoidable.y > paddleBot) || !possiblePosition) {
                        velocityScores.set(i, -1.0);
                    }
                }

                double y = player.y + conf.paddleHeight * 0.5 + 50 * (i - 50) / 50.0;
                if(velocityScores.get(i) > 0) {
                    Visualisation.drawCross(lines, Color.green, 20, y);
                }
                else {
                    Visualisation.drawCross(lines, Color.red, 20, y);
                }
            }

            int index = (int) ((relativeVelocity * 0.5 + 0.5) * 99);
            if(velocityScores.get(index) > 0 || velocityScores.get(35) > 0 || velocityScores.get(65) > 0) {
                // all ok, still easy to dodge the missile.
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
