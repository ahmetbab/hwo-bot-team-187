package redlynx.bots;

import redlynx.bots.finals.FinalSauron;
import redlynx.bots.semifinals.SFSauron;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;

public class PongMatcher {

    // takes no arguments.
    public static void main(String[] args) {
        PongGameBot bot1 = new FinalSauron();
        PongGameBot bot2 = new SFSauron();

        createMatch(bot1, bot2);
    }

    private static void createMatch(final PongGameBot bot1, final PongGameBot bot2) {

        final String[] args1 = {bot1.getDefaultName(), "boris.helloworldopen.fi", "9090","-match", bot2.getDefaultName()};
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
