package ug.go.health.ihrisbiometric.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

import ug.go.health.ihrisbiometric.R;


public class NonScrollableExpandableListView extends ExpandableListView {

    public NonScrollableExpandableListView(Context context) {
        super(context);
    }

    public NonScrollableExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonScrollableExpandableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}