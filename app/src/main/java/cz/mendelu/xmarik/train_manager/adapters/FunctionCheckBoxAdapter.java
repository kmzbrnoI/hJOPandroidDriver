package cz.mendelu.xmarik.train_manager.adapters;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.os.Handler;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.models.TrainFunction;
import cz.mendelu.xmarik.train_manager.activities.TrainHandler;

/**
 * FunctionCheckBoxAdapter is a ListView` checkbox items with behavior connected to function
 * setting.
 */
public class FunctionCheckBoxAdapter extends ArrayAdapter<TrainFunction> {
    private LayoutInflater vi;
    private ArrayList<TrainFunction> trainList;
    private boolean m_enabled;

    public FunctionCheckBoxAdapter(Context context, int textViewResourceId,
                                   ArrayList<TrainFunction> trainList, boolean enabled) {
        super(context, textViewResourceId, trainList);
        this.trainList = new ArrayList<>();
        this.trainList.addAll(trainList);
        this.m_enabled = enabled;
        vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = vi.inflate(R.layout.lok_function, null);
            holder = new ViewHolder();
            holder.code = (TextView) convertView.findViewById(R.id.code);
            holder.chb_func = (CheckBox) convertView.findViewById(R.id.chb_func);
            convertView.setTag(holder);

            holder.chb_func.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chb_onClick((CheckBox)v);
                }
            });

            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox chb = ((CheckBox)v.findViewById(R.id.chb_func));
                    if (chb.isEnabled()) {
                        chb.toggle();
                        chb_onClick(chb);
                    }
                }
            });

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        TrainFunction function = trainList.get(position);

        holder.code.setText(function.name.equals("") ?
             "F" + String.valueOf(function.num) : "F" + String.valueOf(function.num) + ": " + function.name);

        holder.chb_func.setChecked(function.checked);
        holder.chb_func.setTag(function);
        holder.chb_func.setEnabled(m_enabled);

        return convertView;
    }

    void chb_onClick(CheckBox chb) {
        final CheckBox c = chb;
        final TrainFunction trainFunc = (TrainFunction)chb.getTag();
        ((TrainHandler) ((ContextWrapper) chb.getContext())).onFuncChanged(trainFunc.num, chb.isChecked());

        if (trainFunc.type == TrainFunction.TrainFunctionType.MOMENTARY && trainFunc.checked) {
            chb.setEnabled(false);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    c.setChecked(false);
                    ((TrainHandler) ((ContextWrapper)c.getContext())).onFuncChanged(trainFunc.num, c.isChecked());
                    c.setEnabled(true);
                }
            }, 750);
        }
    }

    private class ViewHolder {
        TextView code;
        CheckBox chb_func;
    }

}