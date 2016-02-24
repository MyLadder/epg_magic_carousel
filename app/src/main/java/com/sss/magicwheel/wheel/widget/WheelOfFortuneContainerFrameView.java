package com.sss.magicwheel.wheel.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.sss.magicwheel.R;
import com.sss.magicwheel.wheel.entity.WheelDataItem;
import com.sss.magicwheel.wheel.entity.WheelRotationDirection;
import com.sss.magicwheel.wheel.WheelAdapter;
import com.sss.magicwheel.wheel.WheelComputationHelper;
import com.sss.magicwheel.wheel.decor.WheelFrameItemDecoration;
import com.sss.magicwheel.wheel.manager.AbstractWheelLayoutManager;
import com.sss.magicwheel.wheel.manager.BottomWheelLayoutManager;
import com.sss.magicwheel.wheel.manager.TopWheelLayoutManager;

import java.util.Collections;
import java.util.List;

/**
 * @author Alexey Kovalev
 * @since 10.02.2016.
 */
public final class WheelOfFortuneContainerFrameView extends FrameLayout {

    private final WheelComputationHelper computationHelper;

    private BottomWheelLayoutManager bottomWheelLayoutManager;

    private TopWheelContainerRecyclerView topWheelContainer;
    private BottomWheelContainerRecyclerView bottomWheelContainer;

    private int lastTouchAction;

    private WheelSectorRaysDecorationFrame wheelSectorsRaysDecorationFrame;

    /**
     * We use it as not recycler view item decoration because RecyclerView's
     * containers rotated in order to implement wheel startup animation.
     */
    private final WheelFrameItemDecoration wheelFrameItemDecoration;

    public WheelOfFortuneContainerFrameView(Context context) {
        this(context, null);
    }

    public WheelOfFortuneContainerFrameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelOfFortuneContainerFrameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        computationHelper = WheelComputationHelper.getInstance();
        inflateAndBindContainerView(context);

//        topWheelContainer.setVisibility(INVISIBLE);
//        bottomWheelContainer.setVisibility(INVISIBLE);

        wheelSectorsRaysDecorationFrame.setWheelContainers(topWheelContainer, bottomWheelContainer);
        wheelFrameItemDecoration = new WheelFrameItemDecoration(getContext());

        initBottomWheelContainer(bottomWheelContainer);
        initTopWheelContainer(topWheelContainer);
    }

    private void inflateAndBindContainerView(Context context) {
        inflate(context, R.layout.wheel_container_layout, this);
        topWheelContainer = (TopWheelContainerRecyclerView) findViewById(R.id.top_wheel_container);
        bottomWheelContainer = (BottomWheelContainerRecyclerView) findViewById(R.id.bottom_wheel_container);
        wheelSectorsRaysDecorationFrame = (WheelSectorRaysDecorationFrame) findViewById(R.id.wheel_decoration_frame);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        wheelFrameItemDecoration.onDraw(canvas, null, null);
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    /**
     * We dispatch touch event only to the top wheel. This wheel will
     * play role of MASTER in MASTER-SLAVE couple where SLAVE would
     * be bottom wheel.
     * <p/>
     * Bottom wheel will receive scroll notifications from MASTER via
     * {@link com.sss.magicwheel.wheel.manager.TopWheelLayoutManager.WheelOnScrollingCallback}
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // in order to dispatch click event to sectorView inside bottom wheel
        final int actionMasked = MotionEventCompat.getActionMasked(event);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                bottomWheelContainer.dispatchTouchEvent(event);
                break;
            case MotionEvent.ACTION_UP:
                if (lastTouchAction != MotionEvent.ACTION_MOVE) {
                    bottomWheelContainer.dispatchTouchEvent(event);
                }
                break;
        }

        lastTouchAction = actionMasked;
        topWheelContainer.dispatchTouchEvent(event);
        return true;
    }

    public void swapData(List<WheelDataItem> newData) {
        final List<WheelDataItem> unmodifiableNewData = Collections.unmodifiableList(newData);
        topWheelContainer.getAdapter().swapData(unmodifiableNewData);
        bottomWheelContainer.getAdapter().swapData(unmodifiableNewData);
//        wheelSectorsRaysDecorationFrame.invalidate();
    }

    private void initTopWheelContainer(TopWheelContainerRecyclerView topWheelContainerView) {
        topWheelContainerView.setLayoutManager(new TopWheelLayoutManager(
                getContext(), topWheelContainer, computationHelper,
                new AbstractWheelLayoutManager.WheelOnInitialLayoutFinishingListener() {
                    @Override
                    public void onInitialLayoutFinished(int finishedAtAdapterPosition) {
                        bottomWheelLayoutManager.setStartLayoutFromAdapterPosition(finishedAtAdapterPosition);
                    }
                },
                new TopWheelLayoutManager.WheelOnScrollingCallback() {
                    @Override
                    public void onScrolledBy(int dy) {
                        bottomWheelContainer.scrollBy(0, dy);
                    }
                }
        ));
        topWheelContainerView.setAdapter(createEmptyWheelAdapter(new WheelAdapter.OnWheelItemClickListener() {
            @Override
            public void onItemClicked(View clickedSectorView, WheelDataItem dataItem) {
                topWheelContainer.handleTapOnSectorView(clickedSectorView);
            }
        }));

        addTopWheelItemDecorations(topWheelContainerView);
    }

    private void initBottomWheelContainer(BottomWheelContainerRecyclerView bottomWheelContainerView) {
        bottomWheelLayoutManager = new BottomWheelLayoutManager(
                getContext(), bottomWheelContainer, computationHelper, null,
                new AbstractWheelLayoutManager.WheelOnStartupAnimationListener() {
                    @Override
                    public void onAnimationUpdate(AbstractWheelLayoutManager.WheelStartupAnimationStatus animationStatus) {
                        if (animationStatus == AbstractWheelLayoutManager.WheelStartupAnimationStatus.InProgress) {
                            wheelSectorsRaysDecorationFrame.invalidate();
                        }
                    }
                });
        bottomWheelContainerView.setBottomWheelSectorTapListener(new BottomWheelContainerRecyclerView.OnBottomWheelSectorTapListener() {
            @Override
            public void onRotateWheelByAngle(double rotationAngleInRad) {
                // dispatch rotation to top wheel because it's MASTER and bottom
                // wheel will be rotated automatically because it's SLAVE
                topWheelContainer.smoothRotateWheelByAngleInRad(rotationAngleInRad, WheelRotationDirection.Anticlockwise);
            }
        });
        bottomWheelContainerView.setLayoutManager(bottomWheelLayoutManager);
        bottomWheelContainerView.setAdapter(createEmptyWheelAdapter(new WheelAdapter.OnWheelItemClickListener() {
            @Override
            public void onItemClicked(View clickedSectorView, WheelDataItem dataItem) {
                bottomWheelContainer.handleTapOnSectorView(clickedSectorView);
            }
        }));

        addBottomWheelItemDecorations(bottomWheelContainerView);
    }

    private void addTopWheelItemDecorations(RecyclerView wheelContainerView) {
//        wheelContainerView.addItemDecoration(new WheelFrameItemDecoration(getContext()));
//        wheelContainerView.addItemDecoration(new WheelSectorRayItemDecoration(getContext()));
//        wheelContainerView.addItemDecoration(new WheelSectorLeftEdgeColorItemDecoration(getActivity()));
    }

    private void addBottomWheelItemDecorations(RecyclerView wheelContainerView) {
//        wheelContainerView.addItemDecoration(new WheelFrameItemDecoration(getContext()));
//        wheelContainerView.addItemDecoration(new WheelSectorRayItemDecoration(getContext()));
//        wheelContainerView.addItemDecoration(new WheelSectorLeftEdgeColorItemDecoration(getActivity()));
    }

    private WheelAdapter createEmptyWheelAdapter(WheelAdapter.OnWheelItemClickListener clickHandler) {
        return new WheelAdapter(getContext(), Collections.<WheelDataItem>emptyList(), clickHandler);
    }

}