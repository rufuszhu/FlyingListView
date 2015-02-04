/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.rufuszhu.listviewflyinflyoutanimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This application creates a ListView to which new elements can be added from the
 * top. When a new element is added, it is animated from above the bounds
 * of the list to the top. When the list is scrolled all the way to the top and a new
 * element is added, the row animation is accompanied by an image animation that pops
 * out of the round view and pops into the correct position in the top cell.
 */
public class InsertingCells extends Activity {
    private static final String TAG = "InsertingCells";

    private static final int MOVE_DURATION = 150;


    private ListView mListView;


    private Integer mItemNum = 0;



    private StableArrayAdapter mAdapter;

    HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ArrayList<String> abc = new ArrayList<String>();
        mAdapter = new StableArrayAdapter(this,R.layout.opaque_text_view, abc);

        mListView = (ListView)findViewById(R.id.listview);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,final View v, int position, long id) {
                v.animate().setDuration(MOVE_DURATION).
                        alpha(0).translationX(-v.getWidth()).
                        withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                // Restore animated values
                                v.setAlpha(1);
                                v.setTranslationX(0);
                                animateRemoval(mListView, v);
                            }
                        });
            }
        });
    }


    public void addRow(View view) {
        //mButton.setEnabled(false);

        mItemNum++;

        mAdapter.add(mItemNum+" aabbcc");
        mAdapter.updatemIdMap();

        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);

                int count = mListView.getCount() - 1;
                //do not need to animate if not visible
                if(count<0 || count <= mListView.getLastVisiblePosition())
                {
                    View view = mListView.getChildAt(count);
                    view.setTranslationX(-view.getWidth());
                    view.animate().setDuration(MOVE_DURATION).
                            alpha(1).translationX(0);
                }

                return true;
            }
        });


    }


    /**
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     */
    private void animateRemoval(final ListView listview, View viewToRemove) {
        int firstVisiblePosition = listview.getFirstVisiblePosition();
        for (int i = 0; i < listview.getChildCount(); ++i) {
            View child = listview.getChildAt(i);
            if (child != viewToRemove) {
                int position = firstVisiblePosition + i;
                long itemId = mAdapter.getItemId(position);
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
        // Delete the item from the adapter
        int position = mListView.getPositionForView(viewToRemove);
        mAdapter.remove(mAdapter.getItem(position));

        final ViewTreeObserver observer = listview.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                int firstVisiblePosition = listview.getFirstVisiblePosition();
                for (int i = 0; i < listview.getChildCount(); ++i) {
                    final View child = listview.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = mAdapter.getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop != null) {
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation) {
                                child.animate().withEndAction(new Runnable() {
                                    public void run() {
                                        mListView.setEnabled(true);
                                    }
                                });
                                firstAnimation = false;
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        int childHeight = child.getHeight() + listview.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        if (firstAnimation) {
                            child.animate().withEndAction(new Runnable() {
                                public void run() {
                                    mListView.setEnabled(true);
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }
}
