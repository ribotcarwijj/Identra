package dev.identra.app.x.hook.interceptors.hardware.camera;

import dev.identra.app.x.data.utils.random.RandomGenerator;
import dev.identra.app.x.core.settings.random_old.interfaces.IRandomizer;
import dev.identra.app.x.core.settings.random_old.RandomElement;

public class RandomCameraCount extends RandomElement {
    public static IRandomizer create() { return new RandomCameraCount(); }
    public RandomCameraCount() {
        super("Camera Count");
        bindSetting("hardware.camera.count");
    }

    @Override
    public String generateString() { return String.valueOf(RandomGenerator.nextInt(0, 4)); }
}