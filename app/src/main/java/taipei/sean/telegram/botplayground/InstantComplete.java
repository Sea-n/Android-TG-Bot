package taipei.sean.telegram.botplayground;

import android.content.Context;

public class InstantComplete extends android.support.v7.widget.AppCompatAutoCompleteTextView {
    public InstantComplete(Context context) {
        super(context);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }
}
