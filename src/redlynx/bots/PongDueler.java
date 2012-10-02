package redlynx.bots;

import redlynx.bots.preliminaries.dataminer.DataMiner;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;

public class PongDueler {
    public static void main(String[] args) {
        PongGameBot bot1 = new DataMiner("miner1");
        bot1.setName("miner1");
        // createMatch(bot1);
    }

    private static void createMatch(final PongGameBot bot1) {
        String opponentName = "pukku";
        final String[] args1 = {"Sauron", "boris.helloworldopen.fi", "9090", "-vis", "-match", opponentName};
        Pong.init(args1, bot1);
    }
}
