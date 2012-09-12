package redlynx.pong.collisionmodel;

import redlynx.pong.Vector2;

import java.io.InputStream;
import java.util.Scanner;

public class PongModelInitializer {

    public static void init(PongModel model, InputStream data) {
        Scanner scanner = new Scanner(data);

        while(scanner.hasNext())
        {
            String type = scanner.next();
            double vx_in=0, vy_in=0;

            while(type.equals("Ball")) {
                vx_in = scanner.nextDouble();
                vy_in = scanner.nextDouble();
                type = scanner.next();
            }

            double position = scanner.nextDouble();

            scanner.next();
            double vx_out = scanner.nextDouble();
            double vy_out = scanner.nextDouble();

            model.learn(position, new Vector2(vx_in, vy_in), new Vector2(vx_out, vy_out));
        }

    }
}
