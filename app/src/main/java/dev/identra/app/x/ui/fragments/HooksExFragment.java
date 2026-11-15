package dev.identra.app.x.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.identra.app.DebugUtil;
import dev.identra.app.R;
import dev.identra.app.databinding.HooksExFragmentBinding;
import dev.identra.app.databinding.HooksExItemBinding;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.data.utils.TryRun;
import dev.identra.app.x.ui.adapters.hooks.HookAdapter;
import dev.identra.app.x.ui.adapters.hooks.elements.ResultRequest;
import dev.identra.app.x.ui.adapters.hooks.elements.XHook;
import dev.identra.app.x.ui.adapters.hooks.elements.XHookIO;
import dev.identra.app.x.ui.core.CoreUiColors;
import dev.identra.app.x.ui.core.CoreUiLog;
import dev.identra.app.x.ui.core.DataEventKind;
import dev.identra.app.x.ui.core.UserClientAppContext;
import dev.identra.app.x.ui.core.fragment.ListFragment;
import dev.identra.app.x.ui.core.util.FileOpenUtils;
import dev.identra.app.x.ui.core.util.ListFragmentUtils;
import dev.identra.app.x.ui.core.view_registry.SettingSharedRegistry;
import dev.identra.app.x.ui.core.view_registry.SharedRegistry;
import dev.identra.app.x.ui.dialogs.ConfirmDialog;
import dev.identra.app.x.ui.dialogs.HookEditDialog;
import dev.identra.app.x.ui.dialogs.HooksDialog2;
import dev.identra.app.x.ui.models.HooksExItemViewModel;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.call.PutHookExCommand;


import me.zhanghai.android.fastscroll.FastScrollerBuilder;


public class HooksExFragment
        extends
        ListFragment<XHook, HooksExFragmentBinding, HooksExItemBinding>
        implements
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = LibUtil.generateTag(HooksExFragment.class);
    public static HooksExFragment newInstance(UserClientAppContext context) { return ListFragmentUtils.newInstance(HooksExFragment.class, context); }

    private final SettingSharedRegistry sharedRegistry = new SettingSharedRegistry();

    @Override
    public SharedRegistry getSharedRegistry() { return sharedRegistry; }
    @Override
    public Fragment getAsFragment() {
        return this;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.createViewModel(HooksExItemViewModel.class, true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        super.ensureHasUserContext();
        super.setAdapter(new HookAdapter(requireContext(), null, this, getUserContext().bindShared(sharedRegistry)));

        super.initFloatingActions(
                binding.flSettingsButtonOne,
                binding.flSettingsButtonTwo,
                binding.flSettingsButtonThree);

        super.setProgressBar(binding.pbHookz);
        super.initSwipeRefreshLayout(binding.swipeRefreshHookz, CoreUiColors.getSwipeRefreshColor(requireContext()));
        super.initRecyclerView(binding.rvHookz);

        new FastScrollerBuilder(binding.rvHookz).useMd2Style().build();

        super.startObserver();
        wire();
    }

    @Override
    protected void dataEvent(DataEventKind kind, List<XHook> xHooks) {

    }

    @Override
    public void wire() {
        super.wire();
        binding.flSettingsButtonOne.setOnClickListener(this);
        binding.flSettingsButtonTwo.setOnClickListener(this);
        binding.flSettingsButtonThree.setOnClickListener(this);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null || resultCode != Activity.RESULT_OK) return;
        Uri uri = data.getData();
        if (uri == null) return;
        switch (requestCode) {
            case ConfUtils.REQUEST_OPEN_CONFIG:
                TryRun.onMain(() -> {
                    List<XHook> hooks = FileOpenUtils.readJsonElementsFromUri(requireContext(), uri, "hooks", XHook.class);
                    if(!ListUtil.isValid(hooks)) {
                        Log.e(TAG, "Failed to Read Hooks Count=" + ListUtil.size(hooks));
                        return;
                    }

                    final HookAdapter adapter = (HookAdapter)getAdapter().getAsListAdapterOrNull();
                    if(adapter == null) {
                        Log.e(TAG, "Adapter is null!");
                        return;
                    }

                    HooksDialog2.create()
                            .set(hooks, requireContext())
                            .setEvent((e, d) -> {
                                if(DebugUtil.isDebug()) Log.d(TAG, "Enabled=" + e.size() + " Disabled=" + d.size());
                                if(ListUtil.isValid(e)) {
                                    List<XHook> fromList = adapter.getCurrentList();
                                    Map<String, XHook> items = new HashMap<>();
                                    if(ListUtil.isValid(fromList)) {
                                        for(XHook hook : fromList) {
                                            if(hook.isValid())
                                                items.put(hook.getObjectId(), hook);
                                        }
                                    }

                                    if(DebugUtil.isDebug())
                                        Log.d(TAG, Str.fm("Applying Total of [%s] to a List of Existing Hooks of Count [%s], Disabled [%s]",
                                                e.size(),
                                                fromList.size(),
                                                d.size()));

                                    int successful = 0;
                                    int failed = 0;
                                    for(XHook imported : e) {
                                        if(imported.isValid()) {
                                            ResultRequest res = PutHookExCommand.putEx(getContext(), imported, false);
                                            if(res.successful()) {
                                                successful++;
                                                items.put(res.hook.getObjectId(), res.hook);
                                            }
                                            else {
                                                failed++;
                                                if(DebugUtil.isDebug())
                                                    Log.d(TAG, Str.fm("Failed to Import Hook [%s] Error=%s",
                                                            imported.getObjectId(),
                                                            res.exception));
                                            }
                                        }
                                    }

                                    if(DebugUtil.isDebug())
                                        Log.d(TAG, Str.fm("Applying total of [%s] Hooks from Enabled [%s] Disabled [%s] Successful [%s] Failed [%s]",
                                                items.size(),
                                                e.size(),
                                                d.size(),
                                                successful,
                                                failed));


                                    adapter.submitList(new ArrayList<>(items.values()));
                                    Snackbar.make(requireView(), Str.fm(getString(R.string.msg_imported_hooks_success), successful), Snackbar.LENGTH_LONG).show();
                                }
                            })
                            .show(getFragmentMan(), requireContext().getString(R.string.menu_filter_hooks));
                });
                break;
            case ConfUtils.REQUEST_SAVE_CONFIG:
                TryRun.onMain(() -> {
                    XHook hook =  sharedRegistry.pop(XHook.class);
                    ConfUtils.takePersistablePermissions(requireContext(), uri);
                    boolean success = FileOpenUtils.writeJsonElementToUri(requireContext(), uri, hook);
                    Snackbar.make(requireView(), success ?
                            getString(R.string.msg_export_hook_success) :
                            getString(R.string.msg_export_hook_error), Snackbar.LENGTH_LONG).show();
                });
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        int id = compoundButton.getId();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        final int id = CoreUiLog.getViewIdOnClick(v, TAG);
        final Context context = v.getContext();
        switch (id) {
            case R.id.flSettingsButtonOne:
                recyclerViewWrapper.getFloatingActionButtonContext().invokeFloatingActions();
                break;
            case R.id.flSettingsButtonTwo:
                ConfUtils.startConfigFilePicker(this);
                break;
            case R.id.flSettingsButtonThree:
                TryRun.onMain(() -> {
                    HookAdapter adapter = (HookAdapter)getAdapter().getAsListAdapterOrNull();
                    if(adapter != null) {
                        HookEditDialog.create()
                                .setEditListener((hook) -> {
                                    if(hook != null && hook.isValid() && !Str.isEmpty(hook.group)) {
                                        ResultRequest res = PutHookExCommand.putEx(context, hook, false);
                                        if(res.successful())
                                            adapter.onHookEdited(null, res.hook, false);

                                        Log.d(TAG, "Hook Sent Status " + res.successful() + " Hook=" + hook.getObjectId());
                                        Snackbar.make(requireView(), res.successful() ?
                                                getString(R.string.msg_create_hook_success) :
                                                getString(R.string.msg_create_hook_error), Snackbar.LENGTH_LONG).show();
                                    }
                                }).show(getFragmentMan(), context.getString(R.string.title_hook_edit));
                    }
                });
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onLongClick(View v) {
        int id = CoreUiLog.getViewIdOnLongClick(v, TAG);
        return false;
    }

    public Context tryGetContext() {
        try {
            return requireContext();
        }catch (Exception ignored) {
            return getContext();
        }
    }
}
