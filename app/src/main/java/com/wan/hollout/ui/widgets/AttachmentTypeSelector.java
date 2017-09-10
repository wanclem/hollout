package com.wan.hollout.ui.widgets;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ViewFlipper;

import com.esotericsoftware.kryo.NotNull;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.wan.hollout.R;
import com.wan.hollout.callbacks.DoneCallback;
import com.wan.hollout.listeners.OnSingleClickListener;
import com.wan.hollout.utils.ApiUtils;
import com.wan.hollout.utils.UiUtils;
import com.wan.hollout.utils.ViewUtil;

import org.json.JSONObject;

import java.util.List;

public class AttachmentTypeSelector extends PopupWindow {

    public static final int ADD_IMAGE = 1;
    public static final int ADD_VIDEO = 2;
    public static final int OPEN_GALLERY = 3;
    public static final int ADD_CONTACT = 4;
    public static final int ADD_DOCUMENT = 5;
    public static final int ADD_LOCATION = 6;
    public static final int ADD_GIF = 7;

    private static final int ANIMATION_DURATION = 300;

    private final
    @NonNull
    ImageView imageButton;
    private final
    @NonNull
    ImageView galleryButton;
    private final
    @NonNull
    ImageView videoButton;
    private final
    @NonNull
    ImageView contactButton;
    private final
    @NonNull
    ImageView documentButton;
    private final
    @NonNull
    ImageView locationButton;
    private final
    @NonNull
    ImageView gifButton;
    private final
    @NonNull
    ImageView closeButton;

    private
    @NonNull
    EditText gifSearchBox;

    private
    @NonNull
    ViewFlipper contentFlipper;

    private
    @NotNull
    RecyclerView gifRecyclerView;

    private
    @NonNull
    ProgressWheel gifLoadingProgressWheel;

    private
    @Nullable
    View currentAnchor;

    private
    @Nullable
    AttachmentClickedListener listener;

    public AttachmentTypeSelector(@NonNull Context context, @NonNull LoaderManager loaderManager, @Nullable AttachmentClickedListener listener) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.attachment_type_selector, null, true);
        RecentPhotoViewRail recentPhotos = ViewUtil.findById(layout, R.id.recent_photos);

        this.listener = listener;
        this.imageButton = ViewUtil.findById(layout, R.id.photo_button);
        this.galleryButton = ViewUtil.findById(layout, R.id.gallery_button);
        this.videoButton = ViewUtil.findById(layout, R.id.video_button);
        this.contactButton = ViewUtil.findById(layout, R.id.contact_button);
        this.documentButton = ViewUtil.findById(layout, R.id.document_button);
        this.locationButton = ViewUtil.findById(layout, R.id.location_button);
        this.gifButton = ViewUtil.findById(layout, R.id.giphy_button);
        this.closeButton = ViewUtil.findById(layout, R.id.close_button);
        this.contentFlipper = ViewUtil.findById(layout, R.id.content_flipper);

        ImageView dummySearchGifImageView = ViewUtil.findById(layout, R.id.dummy_search_image_view);

        this.gifSearchBox = ViewUtil.findById(layout, R.id.gif_search_box);
        this.gifRecyclerView = ViewUtil.findById(layout, R.id.gif_recycler_view);
        this.gifLoadingProgressWheel = ViewUtil.findById(layout, R.id.gif_progress_wheel);

        this.imageButton.setOnClickListener(new PropagatingClickListener(ADD_IMAGE));
        this.galleryButton.setOnClickListener(new PropagatingClickListener(OPEN_GALLERY));
        this.videoButton.setOnClickListener(new PropagatingClickListener(ADD_VIDEO));
        this.contactButton.setOnClickListener(new PropagatingClickListener(ADD_CONTACT));
        this.documentButton.setOnClickListener(new PropagatingClickListener(ADD_DOCUMENT));
        this.locationButton.setOnClickListener(new PropagatingClickListener(ADD_LOCATION));
        this.gifButton.setOnClickListener(new PropagatingClickListener(ADD_GIF));
        this.closeButton.setOnClickListener(new CloseClickListener());

        dummySearchGifImageView.setOnClickListener(new OnSingleClickListener() {

            @Override
            public void onSingleClick(View view) {
                gifSearchBox.requestFocus();
            }

        });

        recentPhotos.setListener(new RecentPhotoSelectedListener());

        setContentView(layout);
        setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        setBackgroundDrawable(new BitmapDrawable());
        setAnimationStyle(0);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setFocusable(true);
        setTouchable(true);

        loaderManager.initLoader(1, null, recentPhotos);

    }

    public void show(final @NonNull View anchor) {
        this.currentAnchor = anchor;

        showAtLocation(anchor, Gravity.BOTTOM, 0, 0);

        getContentView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                getContentView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    animateWindowInCircular(anchor, getContentView());
                } else {
                    animateWindowInTranslate(getContentView());
                }
            }

        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animateButtonIn(imageButton, ANIMATION_DURATION / 2);
            animateButtonIn(documentButton, ANIMATION_DURATION / 2);

            animateButtonIn(galleryButton, ANIMATION_DURATION / 3);
            animateButtonIn(locationButton, ANIMATION_DURATION / 3);
            animateButtonIn(videoButton, ANIMATION_DURATION / 4);
            animateButtonIn(gifButton, ANIMATION_DURATION / 4);
            animateButtonIn(contactButton, 0);
            animateButtonIn(closeButton, 0);
        }

    }

    @Override
    public void dismiss() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animateWindowOutCircular(currentAnchor, getContentView());
        } else {
            animateWindowOutTranslate(getContentView());
        }
    }

    public void setListener(@Nullable AttachmentClickedListener listener) {
        this.listener = listener;
    }

    private void animateButtonIn(View button, int delay) {
        AnimationSet animation = new AnimationSet(true);
        Animation scale = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.0f);

        animation.addAnimation(scale);
        animation.setInterpolator(new OvershootInterpolator(1));
        animation.setDuration(ANIMATION_DURATION);
        animation.setStartOffset(delay);
        button.startAnimation(animation);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateWindowInCircular(@Nullable View anchor, @NonNull View contentView) {
        Pair<Integer, Integer> coordinates = getClickOrigin(anchor, contentView);
        Animator animator = ViewAnimationUtils.createCircularReveal(contentView,
                coordinates.first,
                coordinates.second,
                0,
                Math.max(contentView.getWidth(), contentView.getHeight()));
        animator.setDuration(ANIMATION_DURATION);
        animator.start();
    }

    private void animateWindowInTranslate(@NonNull View contentView) {
        Animation animation = new TranslateAnimation(0, 0, contentView.getHeight(), 0);
        animation.setDuration(ANIMATION_DURATION);

        getContentView().startAnimation(animation);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateWindowOutCircular(@Nullable View anchor, @NonNull View contentView) {
        Pair<Integer, Integer> coordinates = getClickOrigin(anchor, contentView);
        Animator animator = ViewAnimationUtils.createCircularReveal(getContentView(),
                coordinates.first,
                coordinates.second,
                Math.max(getContentView().getWidth(), getContentView().getHeight()),
                0);

        animator.setDuration(ANIMATION_DURATION);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                AttachmentTypeSelector.super.dismiss();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animator.start();
    }

    private void animateWindowOutTranslate(@NonNull View contentView) {
        Animation animation = new TranslateAnimation(0, 0, 0, contentView.getTop() + contentView.getHeight());
        animation.setDuration(ANIMATION_DURATION);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AttachmentTypeSelector.super.dismiss();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        getContentView().startAnimation(animation);
    }

    private Pair<Integer, Integer> getClickOrigin(@Nullable View anchor, @NonNull View contentView) {
        if (anchor == null) return new Pair<>(0, 0);

        final int[] anchorCoordinates = new int[2];
        anchor.getLocationOnScreen(anchorCoordinates);
        anchorCoordinates[0] += anchor.getWidth() / 2;
        anchorCoordinates[1] += anchor.getHeight() / 2;

        final int[] contentCoordinates = new int[2];
        contentView.getLocationOnScreen(contentCoordinates);

        int x = anchorCoordinates[0] - contentCoordinates[0];
        int y = anchorCoordinates[1] - contentCoordinates[1];

        return new Pair<>(x, y);

    }

    public void loadGifs(final Activity activity) {
        UiUtils.toggleFlipperState(contentFlipper, 1);
        ApiUtils.fetchTrendingGifs(new DoneCallback<List<JSONObject>>() {
            @Override
            public void done(List<JSONObject> result, Exception e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }

    private class RecentPhotoSelectedListener implements RecentPhotoViewRail.OnItemClickedListener {
        @Override
        public void onItemClicked(Uri uri) {
            animateWindowOutTranslate(getContentView());
            if (listener != null) listener.onQuickAttachment(uri);
        }
    }

    private class PropagatingClickListener implements View.OnClickListener {

        private final int type;

        private PropagatingClickListener(int type) {
            this.type = type;
        }

        @Override
        public void onClick(View v) {
            if (v.getId() != R.id.giphy_button) {
                animateWindowOutTranslate(getContentView());
            }
            if (listener != null) listener.onClick(type);
        }

    }

    @NonNull
    public ViewFlipper getContentFlipper() {
        return contentFlipper;
    }

    private class CloseClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            dismiss();
        }

    }

    public interface AttachmentClickedListener {

        void onClick(int type);

        void onQuickAttachment(Uri uri);

    }

}
