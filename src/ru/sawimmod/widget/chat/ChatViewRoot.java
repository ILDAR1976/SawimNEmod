package ru.sawimmod.widget.chat;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.sawimmod.R;
import ru.sawimmod.SawimApplication;
import ru.sawimmod.SawimResources;
import ru.sawimmod.Scheme;
import ru.sawimmod.widget.IcsLinearLayout;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:31
 * To change this template use File | Settings | File Templates.
 */
public class ChatViewRoot extends IcsLinearLayout {

    public ChatViewRoot(Context context, View chatListsView, View chatInputBarView) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(layoutParams);
        addViewInLayout(chatListsView, 0, chatListsView.getLayoutParams());
        addViewInLayout(chatInputBarView, 1, chatInputBarView.getLayoutParams());
    }

    public void update() {
        setDividerDrawable(SawimResources.listDivider);
    }

    public void showHint() {
        hideHint();
        TextView hint = new TextView(getContext());
        hint.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        hint.setTextSize(SawimApplication.getFontSize());
        hint.setGravity(Gravity.CENTER);
        hint.setText(R.string.select_contact);
        addView(hint, 2);
    }

    public void hideHint() {
        if (getChildAt(2) != null) removeViewAt(2);
    }
}
