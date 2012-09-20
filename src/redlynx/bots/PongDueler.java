package redlynx.bots;

import redlynx.bots.magmus.Magmus;
import redlynx.bots.test.TestBot;
import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;

public class PongDueler {
    public static void main(String[] args) {
        BaseBot bot1 = new Magmus();
        createMatch(bot1);
    }

    private static void createMatch(final BaseBot bot1) {
        String opponentName = "keijo";
        final String[] args1 = {bot1.getDefaultName(), "boris.helloworldopen.fi", "9090", "-manual", "-match", opponentName};
        Pong.init(args1, bot1);
    }
}
