package com.station.stationdownloader.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VerticalViewPager extends ViewPager {
    public static final String TAG = "VerticalViewPager";

    public VerticalViewPager(Context context) {
        super(context);
        init();
    }

    public VerticalViewPager(Context context,  AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // The majority of the magic happens here
        setPageTransformer(true, new VerticalPageTransformer());
        // The easiest way to get rid of the overscroll drawing that happens on the left and right
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    private class VerticalPageTransformer implements ViewPager.PageTransformer {

        @Override
        public void transformPage(View view, float position) {

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                view.setAlpha(1);

                // Counteract the default slide transition
                view.setTranslationX(view.getWidth() * -position);

                //set Y position to swipe in from top
                float yPosition = position * view.getHeight();
                view.setTranslationY(yPosition);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    /**
     * Swaps the X and Y coordinates of your touch event.
     */
    private MotionEvent swapXY(MotionEvent ev) {
        float width = getWidth();
        float height = getHeight();

        float newX = (ev.getY() / height) * width;
        float newY = (ev.getX() / width) * height;

        ev.setLocation(newX, newY);

        return ev;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
//        swapXY(ev); // return touch coordinates to original reference frame for any child views
//        return intercepted;
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
//        return super.onTouchEvent(swapXY(ev));
        return false;
    }

    @Override
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();

        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            boolean isChild = false;
            for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                 parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                // This would cause the focus search down below to fail in fun ways.
                final StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                     parent = parent.getParent()) {
                    sb.append(" => ").append(parent.getClass().getSimpleName());
                }
                currentFocused = null;
            } else {
            }
        }

        boolean handled = false;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
                direction);
        if (nextFocused != null && nextFocused != currentFocused) {

            Rect mTempRect = null;
            Field mTempRectField = null;
            Method getChildRectInPagerCoordinates = null;
            try {
                mTempRectField = ViewPager.class.getDeclaredField("mTempRect");
                getChildRectInPagerCoordinates = ViewPager.class.getDeclaredMethod("getChildRectInPagerCoordinates", Rect.class, View.class);
                getChildRectInPagerCoordinates.setAccessible(true);
                mTempRectField.setAccessible(true);
                mTempRect = (Rect) mTempRectField.get(this);
                if (direction == View.FOCUS_UP) {
                    // If there is nothing to the left, or this is causing us to
                    // jump to the right, then what we really want to do is page left.

                    final int nextLeft = ((Rect) getChildRectInPagerCoordinates.invoke(this, mTempRect, nextFocused)).top;
                    final int currLeft = ((Rect) getChildRectInPagerCoordinates.invoke(this, mTempRect, currentFocused)).top;

                    Method pageLeft;
                    try {
                        pageLeft = ViewPager.class.getDeclaredMethod("pageLeft");
                        pageLeft.setAccessible(true);
                        if (currentFocused != null && nextLeft >= currLeft) {
                            handled = (boolean) pageLeft.invoke(this);
                        } else {
                            handled = nextFocused.requestFocus();
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                } else if (direction == View.FOCUS_DOWN) {
                    // If there is nothing to the right, or this is causing us to
                    // jump to the left, then what we really want to do is page right.
                    final int nextLeft = ((Rect) getChildRectInPagerCoordinates.invoke(this, mTempRect, nextFocused)).top;
                    final int currLeft = ((Rect) getChildRectInPagerCoordinates.invoke(this, mTempRect, currentFocused)).top;

                    Method pageRight;
                    try {
                        pageRight = ViewPager.class.getDeclaredMethod("pageRight");
                        pageRight.setAccessible(true);
                        if (currentFocused != null && nextLeft <= currLeft) {
                            handled = (boolean) pageRight.invoke(this);
                        } else {
                            handled = nextFocused.requestFocus();
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        } else if (direction == FOCUS_UP || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.

            Method pageLeft;
            try {
                pageLeft = ViewPager.class.getDeclaredMethod("pageLeft");
                pageLeft.setAccessible(true);
                handled = (boolean) pageLeft.invoke(this);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }


        } else if (direction == FOCUS_DOWN || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            Method pageRight;
            try {
                pageRight = ViewPager.class.getDeclaredMethod("pageRight");
                pageRight.setAccessible(true);
                handled = (boolean) pageRight.invoke(this);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        }
        return handled;
    }

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        boolean handled = false;
//        Log.v(TAG, "dispatchKeyEvent()");
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            switch (event.getKeyCode()) {
//                case KeyEvent.KEYCODE_DPAD_UP:
//                    handled = arrowScroll(FOCUS_UP);
//                    return handled;
//                case KeyEvent.KEYCODE_DPAD_DOWN:
//                    handled = arrowScroll(FOCUS_DOWN);
//                    return handled;
//            }
//        }
//        return super.dispatchKeyEvent(event);
//    }
}