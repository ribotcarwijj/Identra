package dev.identra.app.x.core.settings.random;

import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random.interfaces.IRandomizer;

public class RandomColorIntOption  extends RandomElement {
    public static IRandomizer create() { return new RandomColorIntOption(); }
    @Override
    public String getRawValue() { return String.valueOf(RandomGenUtils.generateTrulyRandomColor()); }

    public RandomColorIntOption() { super("Random Color (ARGB Int)"); }

    @Override
    public void randomize(RandomizerSessionContext context) {
        context.pushValue(context.stack.pop(), String.valueOf(RandomGenUtils.generateTrulyRandomColor()));
    }
}
