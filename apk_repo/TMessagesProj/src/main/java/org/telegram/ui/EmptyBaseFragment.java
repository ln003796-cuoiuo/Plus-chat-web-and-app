package org.pluschat.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.pluschat.ui.ActionBar.BaseFragment;
import org.pluschat.ui.Components.SizeNotifierFrameLayout;

public class EmptyBaseFragment extends BaseFragment {

    @Override
    public View createView(Context context) {
        return fragmentView = new SizeNotifierFrameLayout(context);
    }

}
