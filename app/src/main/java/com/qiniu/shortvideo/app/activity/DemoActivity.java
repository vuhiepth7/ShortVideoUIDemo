package com.qiniu.shortvideo.app.activity;

import android.hardware.Camera;
import android.media.AudioFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.faceunity.FURenderer;
import com.faceunity.OnFUControlListener;
import com.faceunity.entity.Effect;
import com.faceunity.entity.EffectEnum;
import com.faceunity.view.adapter.BaseRecyclerAdapter;
import com.faceunity.view.adapter.EffectRecyclerAdapter;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraPreviewListener;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLFaceBeautySetting;
import com.qiniu.pili.droid.shortvideo.PLFocusListener;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordStateListener;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.qiniu.shortvideo.app.R;
import com.qiniu.shortvideo.app.utils.Config;
import com.qiniu.shortvideo.app.utils.RecordSettings;

import java.util.List;

public class DemoActivity extends AppCompatActivity implements
        PLRecordStateListener,
        PLVideoSaveListener,
        PLVideoFilterListener,
        PLCameraPreviewListener,
        PLFocusListener {

    public static final String PREVIEW_SIZE_RATIO = "PreviewSizeRatio";
    public static final String PREVIEW_SIZE_LEVEL = "PreviewSizeLevel";
    public static final String ENCODING_MODE = "EncodingMode";
    public static final String ENCODING_SIZE_LEVEL = "EncodingSizeLevel";
    public static final String ENCODING_BITRATE_LEVEL = "EncodingBitrateLevel";
    public static final String AUDIO_CHANNEL_NUM = "AudioChannelNum";

    private GLSurfaceView mPreview;
    private ImageView imvAddSticker;
    private LinearLayout bottomView;
    private RecyclerView mRvArMask;
    private ImageView imvCloseMenu;

    private PLShortVideoRecorder mShortVideoRecorder;
    private PLCameraSetting mCameraSetting;
    private PLMicrophoneSetting mMicrophoneSetting;
    private PLRecordSetting mRecordSetting;
    private PLVideoEncodeSetting mVideoEncodeSetting;
    private PLAudioEncodeSetting mAudioEncodeSetting;
    private PLFaceBeautySetting mFaceBeautySetting;

    private FURenderer mFURenderer;
    private int mCameraId;
    private int mInputProp;

    private byte[] mCameraData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        initializedView();

        int previewSizeRatioPos = getIntent().getIntExtra(PREVIEW_SIZE_RATIO, 0);
        int previewSizeLevelPos = getIntent().getIntExtra(PREVIEW_SIZE_LEVEL, 0);
        int encodingModePos = getIntent().getIntExtra(ENCODING_MODE, 0);
        int encodingSizeLevelPos = getIntent().getIntExtra(ENCODING_SIZE_LEVEL, 0);
        int encodingBitrateLevelPos = getIntent().getIntExtra(ENCODING_BITRATE_LEVEL, 0);
        int audioChannelNumPos = getIntent().getIntExtra(AUDIO_CHANNEL_NUM, 0);

        mShortVideoRecorder = new PLShortVideoRecorder();
        mCameraSetting = new PLCameraSetting();


        PLCameraSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCameraSetting.setCameraId(facingId);
        mCameraSetting.setCameraPreviewSizeRatio(RecordSettings.PREVIEW_SIZE_RATIO_ARRAY[previewSizeRatioPos]);
        mCameraSetting.setCameraPreviewSizeLevel(RecordSettings.PREVIEW_SIZE_LEVEL_ARRAY[previewSizeLevelPos]);

        mMicrophoneSetting = new PLMicrophoneSetting();
        mMicrophoneSetting.setChannelConfig(RecordSettings.AUDIO_CHANNEL_NUM_ARRAY[audioChannelNumPos] == 1 ?
                AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);

        mVideoEncodeSetting = new PLVideoEncodeSetting(this);
        mVideoEncodeSetting.setEncodingSizeLevel(RecordSettings.ENCODING_SIZE_LEVEL_ARRAY[encodingSizeLevelPos]);
        mVideoEncodeSetting.setEncodingBitrate(RecordSettings.ENCODING_BITRATE_LEVEL_ARRAY[encodingBitrateLevelPos]);
        mVideoEncodeSetting.setHWCodecEnabled(encodingModePos == 0);
        mVideoEncodeSetting.setConstFrameRateEnabled(true);

        mAudioEncodeSetting = new PLAudioEncodeSetting();
        mAudioEncodeSetting.setHWCodecEnabled(encodingModePos == 0);
        mAudioEncodeSetting.setChannels(RecordSettings.AUDIO_CHANNEL_NUM_ARRAY[audioChannelNumPos]);

        mRecordSetting = new PLRecordSetting();
        mRecordSetting.setMaxRecordDuration(RecordSettings.DEFAULT_MAX_RECORD_DURATION);
        mRecordSetting.setRecordSpeedVariable(true);
        mRecordSetting.setVideoCacheDir(Config.VIDEO_STORAGE_DIR);
        mRecordSetting.setVideoFilepath(Config.RECORD_FILE_PATH);

        mFaceBeautySetting = new PLFaceBeautySetting(1.0f, 0.5f, 0.5f);

        mShortVideoRecorder.prepare(mPreview, mCameraSetting, mMicrophoneSetting, mVideoEncodeSetting,
                mAudioEncodeSetting, null, mRecordSetting);

        mShortVideoRecorder.setRecordStateListener(this);
        mShortVideoRecorder.setFocusListener(this);
        mShortVideoRecorder.setVideoFilterListener(this);
        mShortVideoRecorder.setCameraPreviewListener(this);

        // init faceUnity engine
        mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        mInputProp = getCameraOrientation(mCameraId);
        mFURenderer = new FURenderer
                .Builder(this)
                .inputPropOrientation(mInputProp)
                .build();

        imvAddSticker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomView.getVisibility() == View.GONE) {
                    bottomView.setVisibility(View.VISIBLE);
                    imvAddSticker.setVisibility(View.GONE);
                    initArMask();
                }
            }
        });

        imvCloseMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomView.getVisibility() == View.VISIBLE) {
                    bottomView.setVisibility(View.GONE);
                    imvAddSticker.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void initArMask() {
        mRvArMask.setHasFixedSize(true);
        mRvArMask.setLayoutManager(new GridLayoutManager(this, 5));
        ((SimpleItemAnimator) mRvArMask.getItemAnimator()).setSupportsChangeAnimations(false);
        AnimojiAdapter animojiAdapter = new AnimojiAdapter(EffectEnum.getEffectsByEffectType(Effect.EFFECT_TYPE_ANIMOJI));
        animojiAdapter.setOnItemClickListener(new OnAnimojiItemClickListener());
        animojiAdapter.setItemSelected(0);
        mRvArMask.setAdapter(animojiAdapter);
    }

    @Override
    public void onManualFocusStart(boolean b) {

    }

    @Override
    public void onManualFocusStop(boolean b) {

    }

    @Override
    public void onManualFocusCancel() {

    }

    @Override
    public void onAutoFocusStart() {

    }

    @Override
    public void onAutoFocusStop() {

    }

    @Override
    public void onReady() {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onDurationTooShort() {

    }

    @Override
    public void onRecordStarted() {

    }

    @Override
    public void onSectionRecording(long l, long l1, int i) {

    }

    @Override
    public void onRecordStopped() {

    }

    @Override
    public void onSectionIncreased(long l, long l1, int i) {

    }

    @Override
    public void onSectionDecreased(long l, long l1, int i) {

    }

    @Override
    public void onRecordCompleted() {

    }

    @Override
    public boolean onPreviewFrame(byte[] data, int width, int height, int rotation, int fmt, long tsInNanoTime) {
        mCameraData = data;
        return false;
    }

    @Override
    public void onSurfaceCreated() {
        mFURenderer.loadItems();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onSurfaceDestroy() {
        mFURenderer.destroyItems();
        mCameraData = null;
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, long timeStampNs, float[] transformMatrix) {
        if (mCameraData != null) {
            return mFURenderer.onDrawFrameByFBO(mCameraData, texId, texWidth, texHeight);
        }
        return texId;
    }

    @Override
    public void onSaveVideoSuccess(String s) {

    }

    @Override
    public void onSaveVideoFailed(int i) {

    }

    @Override
    public void onSaveVideoCanceled() {

    }

    @Override
    public void onProgressUpdate(float v) {

    }

    private class OnAnimojiItemClickListener implements BaseRecyclerAdapter.OnItemClickListener<Effect> {
        private int mLastPosition = 0;

        @Override
        public void onItemClick(BaseRecyclerAdapter<Effect> adapter, View view, int position) {
            Effect effect = adapter.getItem(position);
            if (mLastPosition != position) {
                if (mFURenderer != null) {
                    mFURenderer.onEffectSelected(effect);
                }
            }
            mLastPosition = position;
        }
    }

    public class AnimojiAdapter extends BaseRecyclerAdapter<Effect> {

        public AnimojiAdapter(@NonNull List<Effect> data) {
            super(data, R.layout.layout_animoji_recycler);
        }

        @Override
        protected void bindViewHolder(BaseViewHolder viewHolder, Effect item) {
            viewHolder.setImageResource(R.id.iv_anim_item, item.resId());
        }

        @Override
        protected void handleSelectedState(BaseViewHolder viewHolder, Effect data, boolean selected) {
            viewHolder.setBackground(R.id.iv_anim_item, selected ? R.drawable.effect_select : 0);
        }
    }

    private EffectRecyclerAdapter.OnEffectSelectListener mOnEffectSelectListener = new EffectRecyclerAdapter.OnEffectSelectListener() {
        @Override
        public void onEffectSelected(Effect effect) {
            if (mFURenderer != null) {
                mFURenderer.onEffectSelected(effect);
            }
        }

        @Override
        public void onMusicFilterTime(long time) {
            if (mFURenderer != null) {
                mFURenderer.onMusicFilterTime(time);
            }
        }
    };

    private void initializedView() {
        mPreview = findViewById(R.id.preview);
        imvAddSticker = findViewById(R.id.imvAddSticker);
        bottomView = findViewById(R.id.bottomView);
        mRvArMask = findViewById(R.id.rvArMask);
        imvCloseMenu = findViewById(R.id.imvCloseMenu);
    }

    private PLCameraSetting.CAMERA_FACING_ID chooseCameraFacingId() {
        if (PLCameraSetting.hasCameraFacing(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (PLCameraSetting.hasCameraFacing(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }

    public int getCameraOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        Log.d("orientation", info.orientation + "");
        return info.orientation;
    }

}
