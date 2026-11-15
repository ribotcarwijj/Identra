package dev.identra.app.x.ui.adapters.settings;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.DebugUtil;
import dev.identra.app.R;
import dev.identra.app.databinding.SettingsExItemBinding;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.data.utils.TryRun;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.ui.core.UINotifier;
import dev.identra.app.x.ui.core.util.CoreUiUtils;
import dev.identra.app.x.ui.core.view_registry.ChangedStatesPacket;
import dev.identra.app.x.ui.core.view_registry.IStateChanged;
import dev.identra.app.x.ui.core.view_registry.SettingSharedRegistry;
import dev.identra.app.x.ui.core.view_registry.SharedRegistry;
import dev.identra.app.x.ui.core.adapter.ListViewManager;
import dev.identra.app.x.ui.core.interfaces.IStateManager;
import dev.identra.app.x.ui.dialogs.SettingsSearchDialog;
import dev.identra.app.x.ui.dialogs.TimePairsDialog;
import dev.identra.app.x.ui.dialogs.wifi.WifiListDialog;
import dev.identra.app.x.ui.dialogs.wifi.XWifiNetwork;
import dev.identra.app.x.ui.dialogs.wifi.XWifiUtils;
import dev.identra.app.x.ui.fragments.SettingFragmentUtils;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.settings.SettingHolder;
import dev.identra.app.x.core.settings.SettingsContainer;
import dev.identra.app.x.core.settings.random.RandomizerSessionContext;
import dev.identra.app.x.core.settings.random.randomizers.RandomizersCache;

public class SettingsListManager extends ListViewManager<SettingHolder, SettingsExItemBinding> {

    private static final String TAG = LibUtil.generateTag(SettingsListManager.class);

    public SettingsListManager(Context context, LinearLayout containerView, IStateManager stateManager) {
        super(context, containerView, stateManager);
    }

    @Override
    protected SettingsExItemBinding inflateItemView(ViewGroup parent) {
        SettingsExItemBinding binding = SettingsExItemBinding.inflate(inflater, parent, false);
        binding.getRoot().setTag(binding);
        return binding;
    }

    @Override
    protected String getStateTag() { return SharedRegistry.STATE_TAG_SETTINGS; }

    @Override
    protected void bindItemView(SettingsExItemBinding binding, SettingHolder setting) {
        if(setting != null) {
            binding.tvSettingExNameNice.setText(Str.getNonNullOrEmptyString(setting.getName(), "null"));
            binding.tiSettingExSettingValue.setText(Str.getNonNullString(setting.getNewValue(), Str.EMPTY));
            setupTextInputEx(binding.tvSettingExNameNice, binding.tiSettingExSettingValue, setting);
            setupCheckbox(binding.cbSettingExEnabled, setting, binding);
        }
    }

    //TODO: Make this shit cleaner I know you hate UI but still...!!!
    private void setupTextInputEx(TextView tvName, EditText textInput, SettingHolder setting) {
        if(RandomizersCache.SETTING_XP_DEFAULTS.equalsIgnoreCase(setting.getName())) {
            setting.setBindings(tvName, textInput, null);
            setting.setNameLabelColor(context);

            //Fix the Index Names
            textInput.setFocusable(false);
            textInput.setFocusableInTouchMode(false);
            textInput.setClickable(true);
            textInput.setCursorVisible(false);
            textInput.setInputType(InputType.TYPE_NULL);

            Drawable arrowDrawable = ContextCompat.getDrawable(textInput.getContext(), android.R.drawable.arrow_down_float);
            textInput.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null);

            textInput.setOnClickListener(view -> {
                SettingsSearchDialog.create()
                        .setSettings(SettingFragmentUtils.getAllSettingsFromFragment(this.stateManager.getAsFragment()))
                        .setCheckedFromValue(setting)
                        .removeParentSettingFromList()
                        .setOnFinishListener((a, b) -> {
                            final String lstString = Str.joinList(b, Str.NEW_LINE);
                            final String base64 = Str.toBase64String(lstString, Str.CHAR_SET_UTF_8);
                            if(DebugUtil.isDebug())
                                Log.d(TAG, Str.fm("Total Settings (%s) were Checked out of (%s) settings",
                                        ListUtil.size(b),
                                        ListUtil.size(a)));

                            //ToDo:
                            //So Pretty I see my flow (almost least this set listener block)
                            //Use Styles such as this, combining TryRun, Dialog Util sm CoreUiUtils etc etc
                            //Just like Str we can make a big Class called Ui or Something >:)
                            setting.setNewValue(base64);
                            TryRun.onMain(() -> {
                                setting.ensureUiUpdated(base64);
                                setting.setNameLabelColor(context);
                                setting.notifyUpdate(stateRegistry.notifier);
                            });
                        })
                        .show(this.stateManager.getFragmentMan(), context.getString(R.string.title_settings_search));
            });
        }
        else if(RandomizersCache.SETTING_NETWORK_ALLOW_LIST.equalsIgnoreCase(setting.getName())) {

            setting.setBindings(tvName, textInput, null);
            setting.setNameLabelColor(context);

            textInput.setFocusable(false);
            textInput.setFocusableInTouchMode(false);
            textInput.setClickable(true);
            textInput.setCursorVisible(false);
            textInput.setInputType(InputType.TYPE_NULL);


            // Optional: Add a dropdown arrow drawable
            Drawable arrowDrawable = ContextCompat.getDrawable(textInput.getContext(), android.R.drawable.arrow_down_float);
            textInput.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null);
            textInput.setOnClickListener(view -> {
                String newVal = setting.getNewValue();
                List<XWifiNetwork> items = XWifiUtils.fromBase64String(newVal);
                if(DebugUtil.isDebug())
                    Log.d(TAG, "Wifi Networks Saved=" + items.size());

                WifiListDialog.create()
                        .setList(items)
                        .setCallback(new WifiListDialog.WifiNetworkCallback() {
                            @Override
                            public void onNetworksUpdated(List<XWifiNetwork> updatedNetworks) {
                                // Save the updated networks list
                                //saveNetworks(updatedNetworks);
                                if(!ListUtil.isValid(updatedNetworks))
                                    return;

                                String newValue = XWifiUtils.toBase64String(updatedNetworks);
                                if(DebugUtil.isDebug())
                                    Log.d(TAG, "Networks Being Saved=" + ListUtil.size(updatedNetworks) + " New Value=" + newValue);

                                setting.setNewValue(newValue);
                                setting.ensureUiUpdated(newValue);
                                setting.setNameLabelColor(context);
                                setting.notifyUpdate(stateRegistry.notifier);
                            }
                        })
                        .show(stateManager.getFragmentMan(), context.getString(R.string.title_wifi_networks));
            });
        }
        else if(CoreUiUtils.SPECIAL_TIME_SETTINGS.contains(setting.getName()) ||
                CoreUiUtils.SPECIAL_TIME_APP_SETTINGS.contains(setting.getName())) {

            setting.setBindings(tvName, textInput, null);
            setting.setNameLabelColor(context);

            textInput.setFocusable(false);
            textInput.setFocusableInTouchMode(false);
            textInput.setClickable(true);
            textInput.setCursorVisible(false);
            textInput.setInputType(InputType.TYPE_NULL);

            // Optional: Add a dropdown arrow drawable
            Drawable arrowDrawable = ContextCompat.getDrawable(textInput.getContext(), android.R.drawable.arrow_down_float);
            textInput.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null);
            textInput.setOnClickListener(view -> {
                String newVal = setting.getNewValue();
                String cleaned = Str.isEmpty(newVal) ? Str.EMPTY : newVal.replaceAll(Str.WHITE_SPACE, Str.EMPTY).toLowerCase();
                boolean isTimeKind = CoreUiUtils.APP_TIME_KINDS.contains(cleaned);
                TimePairsDialog.create()
                        .setTimePairs(isTimeKind ? new ArrayList<>() : Str.splitToList(cleaned)) // Optional: Set initial pairs
                        .setTimePairsFinishListener(timePairs -> {
                            String val = Str.joinList(timePairs);
                            if(Str.isEmpty(val))
                                val = null;

                            setting.setNewValue(val);
                            setting.ensureUiUpdated(val);
                            setting.setNameLabelColor(context);
                            setting.notifyUpdate(stateRegistry.notifier);
                        }).show(stateManager.getFragmentMan(), view.getContext().getString(R.string.title_time_pairs));
            });
        } else {
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s.toString();
                    setting.setNewValue(str);
                    setting.setNameLabelColor(context);
                    setting.notifyUpdate(stateRegistry.notifier);
                }
            };

            setting.setBindings(tvName, textInput, watcher);
            setting.setNameLabelColor(context);

            textInput.addTextChangedListener(watcher);
            //Extra shit
            textInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            });
        }
    }


    private void setupCheckbox(CheckBox checkbox, SettingHolder setting, SettingsExItemBinding binding) {
        SharedRegistry.ItemState state = stateRegistry.getItemState(SharedRegistry.STATE_TAG_SETTINGS, setting.getObjectId());
        checkbox.setChecked(state.isChecked);

        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                stateRegistry.setChecked(SharedRegistry.STATE_TAG_SETTINGS, setting.getObjectId(), isChecked);
                stateRegistry.notifyGroupChange(SettingsContainer.sharedContainerName(setting.getContainerName()), SharedRegistry.STATE_TAG_SETTINGS);
            }
        };

        checkbox.setOnCheckedChangeListener(onCheckedChangeListener);

        stateRegistry.putGroupChangeListener(new IStateChanged() {
            @Override
            public void onGroupChange(ChangedStatesPacket packet) {
                if(packet.isFrom(SharedRegistry.STATE_TAG_CONTAINERS)) {
                    boolean isChecked = stateRegistry.isChecked(SharedRegistry.STATE_TAG_SETTINGS, setting.getObjectId());
                    binding.cbSettingExEnabled.setOnCheckedChangeListener(null);
                    binding.cbSettingExEnabled.setChecked(isChecked);
                    binding.cbSettingExEnabled.setOnCheckedChangeListener(onCheckedChangeListener);
                }
            }
        }, setting.getObjectId());
    }

    @Override
    protected void cleanupItemView(SettingsExItemBinding binding) {
        binding.tiSettingExSettingValue.setOnFocusChangeListener(null);
        binding.tiSettingExSettingValue.addTextChangedListener(null);
        binding.cbSettingExEnabled.setOnCheckedChangeListener(null);
        stateRegistry.putGroupChangeListener(null, SharedRegistry.sharedSettingName(CoreUiUtils.getText(binding.tvSettingExNameNice)));
    }
}