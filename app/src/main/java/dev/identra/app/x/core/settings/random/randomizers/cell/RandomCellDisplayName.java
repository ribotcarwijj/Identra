package dev.identra.app.x.core.settings.random.randomizers.cell;

import java.util.Arrays;
import java.util.List;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class RandomCellDisplayName extends RandomElement {
    public static List<String> NAMES = Arrays.asList(
            "billy",
            "joe",
            "jannet",
            "joey",
            "amanda",
            "random",
            "random-op");

    public RandomCellDisplayName() {
        super("Cell Display Name");
        putIndexSettings(RandomizersCache.SETTING_CELL_DISPLAY_NAME, 1, 2);
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        String op = RandomGenerator.nextElement(NAMES);
        if(op.equals("random")) {
            context.pushSpecial(context.stack.pop(), RandomGenerator.nextString(8, 32));
        } else if(op.equals("random-op")) {
            context.pushSpecial(context.stack.pop(), RandomGenerator.nextElement(RandomOperatorName.CARRIER_NAMES));
        } else {
            context.pushSpecial(context.stack.pop(), op);
        }
    }
}
