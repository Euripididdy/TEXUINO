package test.expandablelistdrawertest;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Stefan on 15/03/2015.
 */
public class CustomListAdapter extends ArrayAdapter<String>
{

    private final Context context;
    private final Boolean heading;
    private final Boolean simple;
    private String[] labels;
    private int[] icons;
    private TextView[] rows;

    public CustomListAdapter(Context context, String[] labels, Boolean heading, Boolean simple)
    {
        super(context, R.layout.list_entry_enhanced, labels);
        this.context = context;
        this.labels = labels;
        this.simple = simple;
        this.heading = heading;
        this.rows = new TextView[labels.length];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        TextView textView;
        ImageView imageView;

        //Simple list
        if (simple)
        {
            rowView = inflater.inflate(R.layout.list_entry_simple, parent, false);
            textView = (TextView) rowView.findViewById(R.id.label);
            formatList(position, this.heading, textView);
            textView.setText(labels[position]);
        }
        //Enhanced list
        else
        {
            if (position == 0 && heading)
            {
                rowView = inflater.inflate(R.layout.list_entry_simple, parent, false);
            }
            else
            {
                rowView = inflater.inflate(R.layout.list_entry_enhanced, parent, false);
                imageView = (ImageView) rowView.findViewById(R.id.logo);
                if (this.icons != null)
                {
                    imageView.setImageResource(icons[position]);
                }
            }
            textView = (TextView) rowView.findViewById(R.id.label);


            formatList(position, this.heading, textView);

            textView.setText(labels[position]);

        }
        this.rows[position] = textView;
        return rowView;
    }

    public TextView getRow(int position)
    {
        return this.rows[position];
    }

    public void setIcons(int[] icons)
    {
        this.icons = icons;
    }

    public void setLabels(String[] labels)
    {
        this.labels = labels;
    }

    public void formatList(int pos, boolean heading, TextView text)
    {
        if (heading && pos == 0)
        {
            text.setBackgroundColor(Color.DKGRAY);
            text.setTextColor(Color.WHITE);
        }
        else
        {
            text.setBackgroundColor(Color.WHITE);
            text.setBackgroundColor(android.R.attr.activatedBackgroundIndicator);
            text.setTextColor(Color.BLACK);
        }
    }

}
