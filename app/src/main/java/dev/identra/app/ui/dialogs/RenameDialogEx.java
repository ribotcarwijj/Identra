package dev.identra.app.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

import dev.identra.app.R;
import dev.identra.app.x.Str;
import dev.identra.app.api.XResult;
import dev.identra.app.api.configs.MockConfig;
import dev.identra.app.logger.XLog;
import dev.identra.app.ui.interfaces.IConfigUpdate;
import dev.identra.app.ui.transactions.ConfigTransactionResult;

public class RenameDialogEx extends AppCompatDialogFragment {
    private Context context;
    private MockConfig config;
    private IConfigUpdate callback;

    public RenameDialogEx setConfig(MockConfig config) { this.config = config; return this; }
    public RenameDialogEx setCallback(IConfigUpdate callback) { this.callback = callback; return this; }

    public RenameDialogEx() { }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View view = inflater.inflate(R.layout.configrename, null);

        final EditText renamedConfig = view.findViewById(R.id.etConfigName);
        final TextView tvConfigOldName = view.findViewById(R.id.tvOldName);
        tvConfigOldName.setText(config.getName());
        builder.setView(view)
                .setTitle(getString(R.string.title_config_rename_config))
                .setNegativeButton(R.string.option_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { XLog.i("Renaming Config Cancelled"); }
                }).setPositiveButton(R.string.option_rename, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(Str.isValidNotWhitespaces(renamedConfig.getText())) {
                            config.setName(renamedConfig.getText().toString());
                            //Check if duplicate names
                            ConfigTransactionResult res = new ConfigTransactionResult();
                            res.context = context;
                            res.code = -1;
                            res.configs.add(config);
                            res.succeeded.add(config);
                            res.result = XResult.create().setSucceeded("Renamed Config");
                            callback.onConfigUpdate(res);
                        }
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
