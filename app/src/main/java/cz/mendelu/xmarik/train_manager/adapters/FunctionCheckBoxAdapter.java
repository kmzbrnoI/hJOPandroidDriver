package cz.mendelu.xmarik.train_manager.adapters;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.activities.EngineController;
import cz.mendelu.xmarik.train_manager.models.EngineFunction;

/**
 * FunctionCheckBoxAdapter is a ListView checkbox items with behavior connected to function
 * setting.
 */
public class FunctionCheckBoxAdapter extends ArrayAdapter<EngineFunction> {
    private final LayoutInflater vi;

    public FunctionCheckBoxAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = vi.inflate(R.layout.lok_function, parent, false);
            holder = new ViewHolder();
            holder.code = convertView.findViewById(R.id.code);
            holder.chb_func = convertView.findViewById(R.id.chb_func);
            convertView.setTag(holder);

            holder.chb_func.setOnClickListener(v -> chb_onClick((CheckBox)v));

            convertView.setOnClickListener(v -> {
                CheckBox chb = v.findViewById(R.id.chb_func);
                if (chb.isEnabled()) {
                    chb.toggle();
                    chb_onClick(chb);
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        EngineFunction function = getItem(position);

        holder.code.setText(function.name.equals("") ?
             "F" + function.num : "F" + function.num + ": " + function.name);

        holder.chb_func.setChecked(function.checked);
        holder.chb_func.setTag(function);

        return convertView;
    }

    private void chb_onClick(CheckBox chb) {
        final CheckBox c = chb;
        final EngineFunction trainFunc = (EngineFunction)chb.getTag();

        onFuncChanged(chb, trainFunc.num);

        if (trainFunc.type == EngineFunction.EngineFunctionType.MOMENTARY && trainFunc.checked) {
            chb.setEnabled(false);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                c.setChecked(false);
                onFuncChanged(c, trainFunc.num);
                c.setEnabled(true);
            }, 750);
        }
    }

    private void onFuncChanged(CheckBox chb, int function) {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof EngineController) {
                ((EngineController) context).onFuncChanged(function, chb.isChecked());
                break;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
    }

    private static class ViewHolder {
        TextView code;
        CheckBox chb_func;
    }

}