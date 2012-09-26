package redlynx.bots;

import redlynx.bots.abSlither.Slither;
import redlynx.bots.jbot.JBot;
import redlynx.bots.magmus.Magmus;
import redlynx.bots.sauron.Sauron;
import redlynx.bots.semifinals.SFSauron;
import redlynx.bots.test.TestBot;
import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;

public class PongMatcher {

    // takes no arguments.
    public static void main(String[] args) {
        BaseBot bot1 = new Sauron();
        BaseBot bot2 = new SFSauron();

        createMatch(bot1, bot2);
    }

    private static void createMatch(final BaseBot bot1, final BaseBot bot2) {

        final String[] args1 = {bot1.getDefaultName(), "boris.helloworldopen.fi", "9090", "-vis", "-match", bot2.getDefaultName()};
        final String[] args2 = {bot2.getDefaultName(), "boris.helloworldopen.fi", "9090", "-vis", "-match", bot1.getDefaultName()};


        new Thread() {
            public void run() {
                Pong.init(args1, bot1);
            }
        }.start();

        new Thread() {
            public void run() {
                Pong.init(args2, bot2);
            }
        }.start();

        // let the games begin!
    }

}
