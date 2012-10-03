package redlynx.pong.util;

import redlynx.pong.client.state.ClientGameState;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class CollisionStorage {

    public static class EndPointCollision {
        Vector2 velocity = new Vector2();
        Vector2 position = new Vector2();

        public EndPointCollision(ClientGameState.Ball ballCopy) {
            velocity.copy(ballCopy.getVelocity());
            position.copy(ballCopy.getPosition());
        }

        public EndPointCollision() {
        }

        public void read(Scanner scanner) {
            position.x = scanner.nextDouble();
            position.y = scanner.nextDouble();
            velocity.x = scanner.nextDouble();
            velocity.y = scanner.nextDouble();
        }
    }
    public static class EndPointChain {

        ArrayList<EndPointCollision> chain = new ArrayList<EndPointCollision>();
        double value;

        public EndPointChain() {
            chain.add(new EndPointCollision());
            chain.add(new EndPointCollision());
            chain.add(new EndPointCollision());
            chain.add(new EndPointCollision());
        }

        public void push(EndPointCollision p) {
            for(int i=0; i<3; ++i) {
                chain.set(i, chain.get(i+1));
            }
            chain.set(3, p);
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(value);
            for(EndPointCollision collision : chain) {
                builder.append(collision.position);
                builder.append(collision.velocity);
            }
            return builder.toString();
        }

        public void read(Scanner scanner) {
            value = scanner.nextDouble();
            for(EndPointCollision collision : chain) {
                collision.read(scanner);
            }
        }

        public void setValue(double value) {
            this.value = value;
        }
    }

    private ArrayList<EndPointChain> chains = new ArrayList<EndPointChain>();
    private EndPointChain activeChain = new EndPointChain();

    public void push(EndPointCollision p) {
        System.out.println("Pushed point");
        activeChain.push(p);
    }

    public void end(boolean won) {
        activeChain.setValue(won ? 1 : -1);
        chains.add(activeChain);
        clear();
    }

    public void clear() {
        activeChain = new EndPointChain();
    }

    public void write(String botName) {
        try {
            FileOutputStream fos = new FileOutputStream(botName + "_collisionChains.dat");
            PrintStream printStream = new PrintStream(fos);

            for(EndPointChain chain : chains) {
                printStream.println(chain);
            }

        } catch (FileNotFoundException e) {
            System.out.println("Could not open collision chain file.");
            return;
        }
    }

    public void read(String botName) {
        try {
            FileInputStream fis = new FileInputStream(botName + "_collisionChains.dat");
            Scanner scanner = new Scanner(fis);
            scanner.useLocale(Locale.US);

            while(scanner.hasNextDouble()) {
                EndPointChain chain = new EndPointChain();
                chain.read(scanner);
                chains.add(chain);
            }

        } catch (FileNotFoundException e) {
            System.out.println("Could not open collision chain file.");
            return;
        }
    }

}
