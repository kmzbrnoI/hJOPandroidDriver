package cz.mendelu.xmarik.train_manager.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

import cz.mendelu.xmarik.train_manager.R;
import cz.mendelu.xmarik.train_manager.TrainFunction;

/**
 * Created by ja on 5. 9. 2016.
 */
public class CheckBoxAdapter extends ArrayAdapter<TrainFunction> {
    private LayoutInflater vi;
    private ArrayList<TrainFunction> trainList;

    public CheckBoxAdapter(Context context, int textViewResourceId,
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
            convertView = vi.inflate(R.layout.trainfunctioninfo, null);
            holder = new ViewHolder();
            holder.code = (TextView) convertView.findViewById(R.id.code);
            holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
            convertView.setTag(holder);
            /*holder.name.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    TrainFunction trainFunc = (TrainFunction) cb.getTag();
                    trainFunc.setSelected(cb.isChecked());
                } TODO
            });*/
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