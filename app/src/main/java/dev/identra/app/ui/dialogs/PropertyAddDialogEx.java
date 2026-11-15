package dev.identra.app.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

import dev.identra.app.R;
import dev.identra.app.api.properties.MockPropPacket;
import dev.identra.app.logger.XLog;
import dev.identra.app.ui.PropertyQue;
import dev.identra.app.ui.interfaces.IPropertyUpdate;
import dev.identra.app.utilities.StringUtil;

public class PropertyAddDialogEx extends AppCompatDialogFragment {
    private EditText edSettingName;
    private EditText edPropertyName;

    private String settingName = null;

    private PropertyQue que;
    private Context context;
    private IPropertyUpdate callback;

    public PropertyAddDialogEx setCallback(IPropertyUpdate callback) { this.callback = callback; return this; }
    public PropertyAddDialogEx setPropertyQue(PropertyQue que) { this.que = que; return this; }
    public PropertyAddDialogEx setContext(Context context)  { this.context = context; return this; }
    public PropertyAddDialogEx setSettingName(String settingName) { this.settingName = settingName; return this; }

    public PropertyAddDialogEx() { }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View view = inflater.inflate(R.layout.propadd, null);
        edSettingName = view.findViewById(R.id.etPropSettingAddSettingName);
        edPropertyName = view.findViewById(R.id.etPropSettingAddName);

        if(StringUtil.isValidAndNotWhitespaces(this.settingName)) {
           edSettingName.setEnabled(false);
           edSettingName.setText(this.settingName);
        }

        builder.setView(view)
                .setTitle(R.string.title_add_property)
                .setNegativeButton(R.string.option_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { XLog.i("Property Add Dialog Was Cancelled!"); }})
                .setPositiveButton(R.string.option_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String propertyName = edPropertyName.getText().toString();
                        String settingName = edSettingName.getText().toString();
                        if(!StringUtil.isValidAndNotWhitespaces(propertyName) || !StringUtil.isValidAndNotWhitespaces(settingName)) {
                            Toast.makeText(context, R.string.error_no_input_property, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(que == null) {
                            XLog.e("Que is NULL for Property Add Dialog...", new Throwable(), true);
                            return;
                        }

                        que.addPropertyMap(context, propertyName, settingName, callback);
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
