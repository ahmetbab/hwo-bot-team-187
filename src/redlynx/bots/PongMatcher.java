package redlynx.bots;

import redlynx.bots.finals.dataminer.DataMiner;
import redlynx.bots.finals.sauron.FinalSauron;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;

public class PongMatcher {

    // takes no arguments.
    public static void main(String[] args) {
        PongGameBot bot2 = new FinalSauron();
        PongGameBot bot1 = new FinalSauron();
        createMatch(bot1, bot2);
    }

    private static void createMatch(final PongGameBot bot1, final PongGameBot bot2) {

        String server = "boris.helloworldopen.fi";
        String local = "localhost";

        final String[] args1 = {bot1.getDefaultName(), server, "9090", "-vis", "-match", bot2.getDefaultName()};
        final String[] args2 = {bot2.getDefaultName(), server, "9090", "-vis", "-match", bot1.getDefaultName()};

        /*
        final String[] args1 = {bot1.getDefaultName(), local, "9090", "-vis"};
        final String[] args2 = {bot2.getDefaultName(), local, "9090", "-vis"};
        */

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
