package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class InstantComplete extends AutoCompleteTextView {
    public InstantComplete(Context context) {
        super(context);
    }

    public InstantComplete(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InstantComplete(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public InstantComplete(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public InstantComplete(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr, defStyleRes, popupTheme);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }
}
