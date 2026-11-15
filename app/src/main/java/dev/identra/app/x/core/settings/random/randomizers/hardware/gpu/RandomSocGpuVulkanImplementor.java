package dev.identra.app.x.core.settings.random.randomizers.hardware.gpu;

import dev.identra.app.x.core.settings.random.RandomElement;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.RandomOptionString;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;
import dev.identra.app.x.core.settings.random.randomizers.hardware.gpu.ran.RanGpuUtils;

public class RandomSocGpuVulkanImplementor extends RandomElement {
    public RandomSocGpuVulkanImplementor() {
        super("Hardware GPU Vulkan Implementor");
        putSettings(RandomizersCache.SETTING_SOC_GPU_VULKAN_IMPLEMENTOR);
        putOptions(RandomOptionString.generate(RanGpuUtils.GPU_VULKAN_IMPLEMENTORS));
    }

    @Override
    public void randomize(RandomizerSessionContext context) {
        //context.stack.push(getFirstSettingName());
        randomOption().randomize(context);
    }
}