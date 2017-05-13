package cz.mendelu.xmarik.train_manager.adapters;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.TrainFunction;
import cz.mendelu.xmarik.train_manager.activities.TrainHandler;

/**
 * FunctionCheckBoxAdapter is a ListView` checkbox itesm with behavior connected to function
 * setting.
 */
public class FunctionCheckBoxAdapter extends ArrayAdapter<TrainFunction> {
    private LayoutInflater vi;
    private ArrayList<TrainFunction> trainList;

    public FunctionCheckBoxAdapter(Context context, int textViewResourceId,
                                   ArrayList<TrainFunction> trainList) {
        super(context, textViewResourceId, trainList);
        this.trainList = new ArrayList<>();
        this.trainList.addAll(trainList);
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
            holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
            convertView.setTag(holder);

            holder.name.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TrainFunction trainFunc = (TrainFunction)((CheckBox)v).getTag();
                    ((TrainHandler)(((ContextWrapper)v.getContext()).getBaseContext())).onFuncChanged(trainFunc.num, ((CheckBox)v).isChecked());
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        TrainFunction function = trainList.get(position);

        holder.code.setText(function.name.equals("") ?
             "F" + String.valueOf(function.num) : "F" + String.valueOf(function.num) + ": " + function.name);

        holder.name.setText("");
        holder.name.setChecked(function.checked);
        holder.name.setTag(function);
        return convertView;
    }

    private class ViewHolder {
        TextView code;
        CheckBox name;
    }

}