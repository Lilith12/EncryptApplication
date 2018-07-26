package com.example.aleksandra.encryptapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class ItemClickSupport implements RecyclerView.OnItemTouchListener {


        private GestureDetector gestureDetector;
        private RecyclerViewClickListener clickListener;

        public ItemClickSupport(Context context, final RecyclerView recyclerView, final RecyclerViewClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onItemLongClicked(recyclerView.getChildAdapterPosition(child), child);
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onItemClicked(rv.getChildAdapterPosition(child), child);
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }

    public interface RecyclerViewClickListener {
        void onItemClicked(int position, View v);
        void onItemLongClicked(int position, View v);
    }

}
