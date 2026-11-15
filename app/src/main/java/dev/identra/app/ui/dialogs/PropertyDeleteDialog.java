package dev.identra.app.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.Objects;

import dev.identra.app.R;
import dev.identra.app.api.properties.MockPropPacket;
import dev.identra.app.api.properties.MockPropSetting;
import dev.identra.app.logger.XLog;
import dev.identra.app.ui.interfaces.IPropertyUpdate;
import dev.identra.app.ui.PropertyQue;

public class PropertyDeleteDialog extends AppCompatDialogFragment {
    private MockPropSetting setting;
    private IPropertyUpdate callback;
    private int adapterPosition;
    private Context context;
    private PropertyQue que;

    public PropertyDeleteDialog addSetting(MockPropSetting propertySetting) { this.setting = propertySetting; return this; }
    public PropertyDeleteDialog addCallback(IPropertyUpdate onCallback) { this.callback = onCallback; return this; }
    public PropertyDeleteDialog addAdapterPosition(int position) { this.adapterPosition = position; return this; }
    public PropertyDeleteDialog addContext(Context context) { this.context = context; return this; }
    public PropertyDeleteDialog addPropertyQue(PropertyQue que) { this.que = que; return this; }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View view = inflater.inflate(R.layout.propdelete, null);
        builder.setView(view)
                .setTitle(R.string.title_delete_property)
                .setNegativeButton(R.string.option_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { XLog.i("Property Deletion Was cancelled!");}})
                .setPositiveButton(R.string.option_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(que != null && setting != null) {
                            que.sendPropertySetting(
                                    context,
                                    setting,
                                    adapterPosition,
                                    MockPropPacket.PROP_NULL,
                                    true,
                                    callback);
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
