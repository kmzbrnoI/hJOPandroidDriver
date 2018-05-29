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
 * FunctionCheckBoxAdapter is a ListView` checkbox itesm with behavior connected to function
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
            holder.name = (CheckBox) convertView.findViewById(R.id.chb_func);
            holder.toggle = (Button) convertView.findViewById(R.id.toggle);
            convertView.setTag(holder);

            holder.name.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TrainFunction trainFunc = (TrainFunction)((CheckBox)v).getTag();
                    ((TrainHandler)((ContextWrapper)v.getContext())).onFuncChanged(trainFunc.num, ((CheckBox)v).isChecked());
                }
            });

            holder.toggle.setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {
                    final TrainFunction trainFunc = (TrainFunction)((Button)v).getTag();
                    ((TrainHandler)((ContextWrapper)v.getContext())).onFuncChanged(trainFunc.num, true);
                    final CheckBox c = (CheckBox) ((View)v.getParent()).findViewById(R.id.chb_func);
                    c.setChecked(true);
                    v.setEnabled(false);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ((TrainHandler)((ContextWrapper)v.getContext())).onFuncChanged(trainFunc.num, false);
                            c.setChecked(false);
                            v.setEnabled(true);
                        }
                    }, 500);
                }
            });

            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox chb = ((CheckBox)v.findViewById(R.id.chb_func));
                    TrainFunction trainFunc = (TrainFunction)chb.getTag();
                    chb.toggle();
                    ((TrainHandler) ((ContextWrapper) v.getContext())).onFuncChanged(trainFunc.num, chb.isChecked());
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
        holder.toggle.setTag(function);

        holder.name.setEnabled(m_enabled);
        holder.toggle.setEnabled(m_enabled);
        convertView.setEnabled(m_enabled);

        return convertView;
    }

    private class ViewHolder {
        TextView code;
        CheckBox name;
        Button toggle;
    }

}