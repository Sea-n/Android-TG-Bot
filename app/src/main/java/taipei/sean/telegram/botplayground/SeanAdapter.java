package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

public class SeanAdapter<T> extends ArrayAdapter<T> implements Filterable {
    // Needed data structures
    private List<T> _objects;
    private List<T> objects;
    private Filter myFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            ArrayList<T> tempList=new ArrayList<>();
            //constraint is the result from text you want to filter against.
            //_objects is your data set you will filter from
            if(constraint != null && _objects!=null) {
                int length=_objects.size();
                for (int i=0; i<length; i++) {
                    T item = _objects.get(i);

                    String lowerItem = item.toString().toLowerCase();
                    String lowerCon = constraint.toString().toLowerCase();

                    if (lowerItem.contains(lowerCon))
                        tempList.add(item);
                }

                //following two lines is very important
                //as publish result can only take FilterResults _objects
                filterResults.values = tempList;
                filterResults.count = tempList.size();
            }
            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence contraint, FilterResults results) {
            objects = (ArrayList<T>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    };

    public SeanAdapter(final Context context, final int tvResId, final List<T> objects) {
        super(context, tvResId, objects);
        this._objects = objects;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return myFilter;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public T getItem(int position) {
        return objects.get(position);
    }
}