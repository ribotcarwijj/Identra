package dev.identra.app.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.identra.app.ActivityBase;
import dev.identra.app.api.XResult;
import dev.identra.app.api.settings.LuaSettingExtended;
import dev.identra.app.api.settings.LuaSettingPacket;
import dev.identra.app.api.core.IdentraCall;
import dev.identra.app.logger.XLog;
import dev.identra.app.ui.dialogs.ISettingDialogListener;
import dev.identra.app.ui.interfaces.ILoader;
import dev.identra.app.ui.interfaces.ISettingTransaction;

public class SettingsDialogCallbackActivity extends ActivityBase implements ISettingDialogListener {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private ILoader loaderFragment;

    public ILoader getLoaderFragment() { return this.loaderFragment; }

    public <T extends ILoader> void initFragmentTransaction(Class<T> loaderClass, int replaceResource, boolean passArgs) {
        setLoaderFragment(loaderClass, passArgs);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(replaceResource, getLoaderFragment().getFragment());
        fragmentTransaction.commit();
    }

    public <T extends ILoader> void setLoaderFragment(Class<T> loaderClass, boolean passArgs) {
        try {
            loaderFragment = loaderClass.newInstance();
            if(passArgs) {
                Fragment frag = loaderFragment.getFragment();
                frag.setArguments(getIntent().getExtras());
            }
        }catch (Exception e) {
            XLog.e("Failed to create Instance of Fragment: class=" + loaderClass.getName(), e, true);
        }
    }

    public Bundle packageToBundle() {
        String packageName = getIntent().getStringExtra("packageName");
        Bundle args = new Bundle();
        args.putString("packageName", packageName);
        return args;
    }

    public void prepareMenuSearch(MenuItem menuSearch) {
        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                XLog.i("Search Text Submitted: text=" + query);
                if (loaderFragment != null) {
                    loaderFragment.filter(query);
                    searchView.clearFocus();
                } return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                XLog.i("Search Text Changed: text=" + newText);
                if (loaderFragment != null) loaderFragment.filter(newText);
                return true;
            }
        });

        menuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) { return true; }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) { return true; }
        });
    }

    @Override
    public void pushSettingPacket(final LuaSettingPacket packet) {
        final Context context = getApplicationContext();
        XLog.i("Packet being sent to bridge: packet=" + packet);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {//lock here ?????
                    final XResult ret = IdentraCall.sendMockSetting(context, packet);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            Toast.makeText(context, ret.getResultMessage(), Toast.LENGTH_SHORT).show();
                            if(loaderFragment != null) loaderFragment.loadData();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void pushSettingPacket(final LuaSettingPacket packet, final LuaSettingExtended original, final int position, final ISettingTransaction transactionCallback) {
        final Context context = getApplicationContext();
        XLog.i("Packet being sent to bridge: packet=" + packet);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {//lock here ?????
                    original.setIsBusy(true);
                    final XResult ret = IdentraCall.sendMockSetting(context, packet);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            Toast.makeText(context, ret.getResultMessage(), Toast.LENGTH_SHORT).show();
                            //if(loaderFragment != null) loaderFragment.loadData();
                            if(transactionCallback != null)
                                transactionCallback.onSettingFinished(original, position, ret);
                            else loaderFragment.loadData();
                        }
                    });
                }
            }
        });
    }
}
