package com.example.v_attendance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ModernListAdapter extends ArrayAdapter<Object> {

    public interface ModernItem {
        String getMainTitle();
        String getSubTitle();
        String getIconText();
    }

    public ModernListAdapter(Context context, List<Object> items) {
        super(context, R.layout.item_modern_list, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_modern_list, parent, false);
        }

        Object item = getItem(position);
        TextView tvIcon = convertView.findViewById(R.id.tvIconText);
        TextView tvTitle = convertView.findViewById(R.id.tvMainTitle);
        TextView tvSub = convertView.findViewById(R.id.tvSubTitle);

        if (item instanceof ModernItem) {
            ModernItem mi = (ModernItem) item;
            tvTitle.setText(mi.getMainTitle());
            tvSub.setText(mi.getSubTitle());
            tvIcon.setText(mi.getIconText());
        } else if (item instanceof String) {
            String s = (String) item;
            if (s.contains(" - ")) {
                String[] parts = s.split(" - ");
                tvTitle.setText(parts[1]);
                tvSub.setText(parts[0]);
                tvIcon.setText(parts[1].substring(0, 1).toUpperCase());
            } else {
                tvTitle.setText(s);
                tvSub.setText("");
                tvIcon.setText(s.substring(0, 1).toUpperCase());
            }
        }

        return convertView;
    }
}
