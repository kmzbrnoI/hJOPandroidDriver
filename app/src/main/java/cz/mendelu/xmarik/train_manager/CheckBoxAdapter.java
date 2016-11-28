package cz.mendelu.xmarik.train_manager;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ja on 5. 9. 2016.
 */
public class CheckBoxAdapter extends ArrayAdapter<TrainFunction> {
    LayoutInflater vi;
    private ArrayList<TrainFunction> trainList;
    private int names;
    public CheckBoxAdapter(Context context, int textViewResourceId,
                           ArrayList<TrainFunction> trainList) {
        super(context, textViewResourceId, trainList);
        this.trainList = new ArrayList<TrainFunction>();
        this.trainList.addAll(trainList);
        vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //TODO nacis data z pameti
        names = Settings.buttons;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        Log.v("ConvertView", String.valueOf(position));
        if (convertView == null) {
            convertView = vi.inflate(R.layout.trainfunctioninfo, null);
            holder = new ViewHolder();
            holder.code = (TextView) convertView.findViewById(R.id.code);
            holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
            convertView.setTag(holder);
            holder.name.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    TrainFunction trainFunc = (TrainFunction) cb.getTag();
                    trainFunc.setSelected(cb.isChecked());
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        TrainFunction function = trainList.get(position);
        String tmpName;
        if (function.getName().equals("")) {
            tmpName = "";
        } else tmpName = function.getName();
        if (names == 1) {
            holder.code.setText(" (" + function.getCode() + ")");
        } else if (names == 2) {
            holder.code.setText(tmpName);
        } else if (names == 3) {
            holder.code.setText(function.getCode() + ": " + tmpName);
        }
        holder.name.setText("");
        holder.name.setChecked(function.isSelected());
        holder.name.setTag(function);
        return convertView;
    }

    private class ViewHolder {
        TextView code;
        CheckBox name;
    }

}