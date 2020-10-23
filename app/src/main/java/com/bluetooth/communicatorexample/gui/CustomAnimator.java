/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bluetooth.communicatorexample.gui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bluetooth.communicatorexample.R;

public class CustomAnimator {

    public void animateIconChange(final ImageView view, final Drawable newIcon, @Nullable final EndListener responseListener) {  // check that the fact that the view gets smaller does not affect the rest of the graphics
        Animation dwindleAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.dwindle_icon);
        dwindleAnimation.setDuration(view.getResources().getInteger(R.integer.durationShort) / 2);
        final Animation enlargeAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.enlarge_icon);
        enlargeAnimation.setDuration(view.getResources().getInteger(R.integer.durationShort) / 2);
        view.startAnimation(dwindleAnimation);
        view.getAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setImageDrawable(newIcon);
                view.startAnimation(enlargeAnimation);
                if (responseListener != null) {
                    view.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            responseListener.onAnimationEnd();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public Animator createAnimatorSize(final View view, int initialWidthInPixels, int initialHeightInPixels, int finalWidthInPixels, int finalHeightInPixels, int duration) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator animatorWidth = createAnimatorWidth(view, initialWidthInPixels, finalWidthInPixels, duration);
        Animator animatorHeight = createAnimatorHeight(view, initialHeightInPixels, finalHeightInPixels, duration);
        animatorSet.play(animatorWidth).with(animatorHeight);

        return animatorSet;
    }

    public Animator createAnimatorWidth(final View view, int initialPixels, int finalPixels, int duration) {
        ValueAnimator animator = ValueAnimator.ofInt(initialPixels, finalPixels);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if(view!=null) {
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.width = (int) valueAnimator.getAnimatedValue();
                    view.setLayoutParams(layoutParams);
                }
            }
        });
        animator.setDuration(duration);

        return animator;
    }

    public Animator createAnimatorHeight(final View view, int initialPixels, int finalPixels, int duration) {
        ValueAnimator animator = ValueAnimator.ofInt(initialPixels, finalPixels);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if(view!=null) {
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.height = (int) valueAnimator.getAnimatedValue();
                    view.setLayoutParams(layoutParams);
                }
            }
        });
        animator.setDuration(duration);

        return animator;
    }


    public abstract static class EndListener {
        public abstract void onAnimationEnd();
    }
}