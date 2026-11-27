package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adapter grid untuk laci aplikasi, mendukung filter pencarian. */
public class AppGridAdapter extends BaseAdapter implements Filterable {

    private final LayoutInflater inflater;
    private final Typeface doto;
    private final List<AppInfo> all = new ArrayList<>();
    private List<AppInfo> shown = new ArrayList<>();
    private final ItemFilter itemFilter = new ItemFilter();
    private int textColor = 0xFFFFFFFF;
    public void setTextColor(int color) { textColor = color; notifyDataSetChanged(); }

    public AppGridAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        doto = Typeface.createFromAsset(context.getAssets(), "fonts/Doto.ttf");
    }

    public void setApps(List<AppInfo> apps) {
        all.clear();
        all.addAll(apps);
        shown = new ArrayList<>(all);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shown.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return shown.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_app, parent, false);
        AppInfo info = shown.get(position);
        ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(info.icon);
        TextView label = view.findViewById(R.id.label);
        label.setTypeface(doto);
        label.setTextColor(textColor);
        label.setText(info.label);
        return view;
    }

    @Override
    public Filter getFilter() {
        return itemFilter;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AppInfo> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(all);
            } else {
                String query = constraint.toString().toLowerCase(Locale.getDefault());
                for (AppInfo app : all) {
                    if (app.label.toLowerCase(Locale.getDefault()).contains(query)) {
                        filtered.add(app);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            shown = (List<AppInfo>) results.values;
            notifyDataSetChanged();
        }
    }
}
