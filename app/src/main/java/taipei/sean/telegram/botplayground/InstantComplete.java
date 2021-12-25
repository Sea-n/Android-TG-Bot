package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.util.AttributeSet;

public class InstantComplete extends androidx.appcompat.widget.AppCompatAutoCompleteTextView {

    public InstantComplete(Context context) {
        super(context);
    }

    public InstantComplete(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InstantComplete(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
