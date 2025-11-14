package rand;

import java.util.Random;
import constants.Const;

public class randomGenerator {
    private static int seed = Const.RANDOM_SEED;
    public static final Random rand = new Random(seed);
}
