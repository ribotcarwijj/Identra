package dev.identra.app.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.identra.app.AppGeneric;
import dev.identra.app.api.XResult;
import dev.identra.app.api.configs.MockConfig;
import dev.identra.app.api.configs.MockConfigPacket;
import dev.identra.app.api.properties.MockPropPacket;
import dev.identra.app.api.settings.LuaSettingExtended;
import dev.identra.app.api.xmock.XMockCall;
import dev.identra.app.logger.XLog;
import dev.identra.app.ui.interfaces.IConfigUpdate;
import dev.identra.app.ui.transactions.ConfigTransactionResult;
import dev.identra.app.ui.transactions.PropTransactionResult;

public class ConfigQue {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private final AppGeneric application;
    private final List<ConfigTransactionResult> results = new ArrayList<>();

    public ConfigQue(AppGeneric application) { this.application = application; }

    public void clearResults() { this.results.clear(); }
    public List<ConfigTransactionResult> getResults() { return this.results; }
    public ConfigTransactionResult getResultAt(int index) { return this.results.get(index); }
    public ConfigTransactionResult getResultFromId(int id) {
        for(ConfigTransactionResult res : this.results) if(res.id == id) return res;
        return null;
    }

    public void sendConfig(
            final Context context,
            final int adapterPosition,
            final MockConfig config,
            final boolean deleteConfig,
            final boolean deleteEnabledSettings,
            final IConfigUpdate onCallback) {

        config.saveValuesFromInput();
        final MockConfigPacket packet = MockConfigPacket.create(config.getName(),
                deleteEnabledSettings ? config.getDisabledSettings() : config.getSettings());

        packet.setCode(deleteConfig ? MockConfigPacket.CODE_DELETE_CONFIG : MockConfigPacket.CODE_INSERT_UPDATE_CONFIG);
        if(packet.getSettings().isEmpty())
            return;

        try {
            XLog.i("Mock Config Packet created! packet=" + packet);
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        final XResult ret = XMockCall.putMockConfig(context, packet);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                ConfigTransactionResult result = new ConfigTransactionResult();
                                result.context = context;
                                result.id = config.getName().hashCode();
                                result.result = ret;
                                result.configs.add(config);
                                result.adapterPosition = adapterPosition;
                                result.code = packet.getCode();
                                result.packets.add(packet);
                                if(ret.succeeded()) result.succeeded.add(config);
                                else if(ret.failed()) result.failed.add(config);
                                results.add(result);
                                if(onCallback != null) onCallback.onConfigUpdate(result);
                            }
                        });
                    }
                }
            });
        }catch (Exception e) {
            XLog.e("Failed to Send Config: config=" + config.getName(), e, true);
            ConfigTransactionResult result = new ConfigTransactionResult();
            result.context = context;
            result.id = config.getName().hashCode();
            result.result = XResult.create().setFailed("Failed to send property error!");
            result.configs.add(config);
            result.adapterPosition = -1;
            result.code = packet.getCode();
            result.packets.add(packet);
            result.failed.add(config);
            results.add(result);
            try { if(onCallback != null) onCallback.onConfigUpdate(result);
            }catch (Exception ex) {  XLog.e("Failed to execute Callback! ", e, true); }
        }
    }
}
