package redlynx.bots;

import redlynx.bots.magmus.Magmus;
import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;

public class PongDueler {
    public static void main(String[] args) {
        BaseBot bot1 = new Magmus();
        createMatch(bot1);
    }

    private static void createMatch(final BaseBot bot1) {
        String opponentName = "harponger";
        final String[] args1 = {"Sauron", "boris.helloworldopen.fi", "9090", "-vis", "-match", opponentName};
        Pong.init(args1, bot1);
    }
}
