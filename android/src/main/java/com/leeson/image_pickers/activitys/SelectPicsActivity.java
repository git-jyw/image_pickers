package com.leeson.image_pickers.activitys;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.leeson.image_pickers.AppPath;
import com.leeson.image_pickers.R;
import com.leeson.image_pickers.utils.CommonUtils;
import com.leeson.image_pickers.utils.GlideEngine;
import com.leeson.image_pickers.utils.ImageCompressEngine;
import com.leeson.image_pickers.utils.ImageCropEngine;
import com.leeson.image_pickers.utils.MeSandboxFileEngine;
import com.leeson.image_pickers.utils.PictureStyleUtil;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.basic.IBridgeViewLifecycle;
import com.luck.picture.lib.basic.PictureSelectorSupporterActivity;
import com.luck.picture.lib.basic.PictureSelectorTransparentActivity;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.dialog.RemindDialog;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;
import com.luck.picture.lib.interfaces.OnPermissionsInterceptListener;
import com.luck.picture.lib.interfaces.OnRequestPermissionListener;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.style.PictureSelectorStyle;
import com.luck.picture.lib.style.SelectMainStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.luck.picture.lib.utils.StyleUtils;
import com.yalantis.ucrop.UCrop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * Created by lisen on 2018-09-11.
 * 只选择多张图片，
 *
 * @author lisen < 453354858@qq.com >
 */
@SuppressWarnings("all")
public class SelectPicsActivity extends BaseActivity {

    private static final int WRITE_SDCARD = 101;

    public static final String GALLERY_MODE = "GALLERY_MODE";
    public static final String UI_COLOR = "UI_COLOR";
    public static final String SHOW_GIF = "SHOW_GIF";
    public static final String SHOW_CAMERA = "SHOW_CAMERA";
    public static final String ENABLE_CROP = "ENABLE_CROP";
    public static final String WIDTH = "WIDTH";
    public static final String HEIGHT = "HEIGHT";
    public static final String COMPRESS_SIZE = "COMPRESS_SIZE";

    public static final String SELECT_COUNT = "SELECT_COUNT";//可选择的数量

    public static final String COMPRESS_PATHS = "COMPRESS_PATHS";//压缩的画
    public static final String CAMERA_MIME_TYPE = "CAMERA_MIME_TYPE";//直接调用拍照或拍视频时有效
    public static final String VIDEO_RECORD_MAX_SECOND = "VIDEO_RECORD_MAX_SECOND";//录制视频最大时间（秒）
    public static final String VIDEO_RECORD_MIN_SECOND = "VIDEO_RECORD_MIN_SECOND";//录制视频最最小时间（秒）
    public static final String VIDEO_SELECT_MAX_SECOND = "VIDEO_SELECT_MAX_SECOND";//选择视频时视频最大时间（秒）
    public static final String VIDEO_SELECT_MIN_SECOND = "VIDEO_SELECT_MIN_SECOND";//选择视频时视频最小时间（秒）
    public static final String LANGUAGE = "LANGUAGE";


    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pics);
        UIUtils.registerEdgeToEdgeAdaptation(getApplication());
        startSel();
    }

    private UCrop.Options buildOptions(PictureSelectorStyle selectorStyle) {
        UCrop.Options options = new UCrop.Options();
        if (selectorStyle != null && selectorStyle.getSelectMainStyle().getStatusBarColor() != 0) {
            SelectMainStyle mainStyle = selectorStyle.getSelectMainStyle();
            boolean isDarkStatusBarBlack = mainStyle.isDarkStatusBarBlack();
            int statusBarColor = mainStyle.getStatusBarColor();
            options.isDarkStatusBarBlack(isDarkStatusBarBlack);
            options.setSkipCropMimeType(new String[]{PictureMimeType.ofGIF(), PictureMimeType.ofWEBP()});
            if (StyleUtils.checkStyleValidity(statusBarColor)) {
                options.setStatusBarColor(statusBarColor);
                options.setToolbarColor(statusBarColor);
            }
            TitleBarStyle titleBarStyle = selectorStyle.getTitleBarStyle();
            if (StyleUtils.checkStyleValidity(titleBarStyle.getTitleTextColor())) {
                options.setToolbarWidgetColor(titleBarStyle.getTitleTextColor());
            }
        }
        return options;
    }

    private int getLang(String language){
        if ("chinese".equals(language)){
            return LanguageConfig.CHINESE;
        }else if ("traditional_chinese".equals(language)){
            return LanguageConfig.TRADITIONAL_CHINESE;
        }else if ("english".equals(language)){
            return LanguageConfig.ENGLISH;
        }else if ("japanese".equals(language)){
            return LanguageConfig.JAPAN;
        }else if ("france".equals(language)){
            return LanguageConfig.FRANCE;
        }else if ("german".equals(language)){
            return LanguageConfig.GERMANY;
        }else if ("russian".equals(language)){
            return LanguageConfig.RU;
        }else if ("vietnamese".equals(language)){
            return LanguageConfig.VIETNAM;
        }else if ("korean".equals(language)){
            return LanguageConfig.KOREA;
        }else if ("portuguese".equals(language)){
            return LanguageConfig.PORTUGAL;
        }else if ("spanish".equals(language)){
            return LanguageConfig.SPANISH;
        }else if ("arabic".equals(language)){
            return LanguageConfig.AR;
        }
        return LanguageConfig.SYSTEM_LANGUAGE;
    }

    private void showLimitedPhotoAccessTipIfNeeded() {
        Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded() called");

        int sdkInt = android.os.Build.VERSION.SDK_INT;
        int targetSdk = android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
        Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded sdkInt=" + sdkInt + ", UPSIDE_DOWN_CAKE=" + targetSdk);

        if (sdkInt < targetSdk) {
            // Android 14 以下不处理
            Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded: SDK < 34, skip dialog");
            return;
        }

        // 是否拥有读取所有图片的权限
        boolean hasReadImages = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED;

        // 是否拥有“仅选定照片访问”的权限（需要在清单中声明 READ_MEDIA_VISUAL_USER_SELECTED）
        boolean hasVisualSelected = false;
        try {
            hasVisualSelected = ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            ) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception ignored) {
            // 低版本 SDK 编译或者常量不可用时忽略
            Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded: exception when checking READ_MEDIA_VISUAL_USER_SELECTED: " + ignored);
        }

        Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded: hasReadImages=" + hasReadImages + ", hasVisualSelected=" + hasVisualSelected);

        // 这里的逻辑含义：如果没有 READ_MEDIA_IMAGES 但有 READ_MEDIA_VISUAL_USER_SELECTED，认为是“部分照片访问”
        if (!hasReadImages && hasVisualSelected) {
            Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded: condition matched, showing tip dialog");
            new AlertDialog.Builder(this)
                    .setMessage("无法访问相册中所有照片，请在系统设置中将「照片」权限改为「所有照片」")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        dialog.dismiss();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                        startActivity(intent);
                    })
                    .setNegativeButton("知道了", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Log.d("getPickerPaths", "showLimitedPhotoAccessTipIfNeeded: condition NOT matched, no dialog. hasReadImages=" + hasReadImages + ", hasVisualSelected=" + hasVisualSelected);
        }
    }


    private void startSel() {

            // showLimitedPhotoAccessTipIfNeeded();


        String mode = getIntent().getStringExtra(GALLERY_MODE);
        Map<String, Number> uiColor = (Map<String, Number>) getIntent().getSerializableExtra(UI_COLOR);

        Number selectCount = getIntent().getIntExtra(SELECT_COUNT, 9);
        boolean showGif = getIntent().getBooleanExtra(SHOW_GIF, true);
        boolean showCamera = getIntent().getBooleanExtra(SHOW_CAMERA, false);
        boolean enableCrop = getIntent().getBooleanExtra(ENABLE_CROP, false);
        Number width = getIntent().getIntExtra(WIDTH, 1);
        Number height = getIntent().getIntExtra(HEIGHT, 1);
        Number compressSize = getIntent().getIntExtra(COMPRESS_SIZE, 500);
        String mimeType = getIntent().getStringExtra(CAMERA_MIME_TYPE);

        Number videoRecordMaxSecond = getIntent().getIntExtra(VIDEO_RECORD_MAX_SECOND, 120);
        Number videoRecordMinSecond = getIntent().getIntExtra(VIDEO_RECORD_MIN_SECOND, 1);
        Number videoSelectMaxSecond = getIntent().getIntExtra(VIDEO_SELECT_MAX_SECOND, 120);
        Number videoSelectMinSecond = getIntent().getIntExtra(VIDEO_SELECT_MIN_SECOND, 1);

        String language = getIntent().getStringExtra(LANGUAGE);


        PictureStyleUtil pictureStyleUtil = new PictureStyleUtil(this);
        pictureStyleUtil.setStyle(uiColor);
        PictureSelectorStyle selectorStyle = pictureStyleUtil.getSelectorStyle();
        //添加图片
        PictureSelector pictureSelector = PictureSelector.create(this);
        if (mimeType != null) {
            //直接调用拍照或拍视频时
            PictureSelector.create(this).openCamera("photo".equals(mimeType) ? SelectMimeType.ofImage() : SelectMimeType.ofVideo())
                    .setRecordVideoMaxSecond(videoRecordMaxSecond.intValue())
                    .setRecordVideoMinSecond(videoRecordMinSecond.intValue())
                    .setLanguage(getLang(language))
                    .setOutputCameraDir(new AppPath(this).getAppVideoDirPath())
                    .setCropEngine((enableCrop) ?
                            new ImageCropEngine(this, buildOptions(selectorStyle), width.intValue(), height.intValue()) : null)
                    .setCompressEngine(new ImageCompressEngine(compressSize.intValue()))
                    ./*setCameraInterceptListener(new OnCameraInterceptListener() {
                @Override
                public void openCamera(Fragment fragment, int cameraMode, int requestCode) {
                    //自定义相机
                    Log.e("TAG", "openCamera: 自定义相机" );
                }
            }).*/setSandboxFileEngine(new MeSandboxFileEngine()).forResult(new OnResultCallbackListener<LocalMedia>() {
                @Override
                public void onResult(ArrayList<LocalMedia> result) {
                    if (result != null && result.size() > 0){
                        LocalMedia localMedia = result.get(0);
                        if ("video".equals(mimeType)){
                            long videoDuration = localMedia.getDuration()/1000;
                            if(videoDuration < videoRecordMinSecond.intValue() || videoDuration > videoRecordMaxSecond.intValue()){
                                String tips = "";
                                if (videoDuration < videoRecordMinSecond.intValue()){
                                    tips = getString(com.luck.picture.lib.R.string.ps_select_video_min_second,videoRecordMinSecond.intValue());
                                }else if (videoDuration > videoRecordMaxSecond.intValue()){
                                    tips = getString(com.luck.picture.lib.R.string.ps_select_video_max_second,videoRecordMaxSecond.intValue());
                                }
                                RemindDialog tipsDialog = RemindDialog.buildDialog(SelectPicsActivity.this,tips);
                                tipsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        Intent intent = new Intent();
                                        intent.putExtra(COMPRESS_PATHS, new ArrayList<>());
                                        setResult(RESULT_OK, intent);
                                        finish();
                                    }
                                });
                                tipsDialog.show();
                            }else{
                                handlerResult(result);
                            }
                        }else{
                            handlerResult(result);
                        }
                    }else{
                        Intent intent = new Intent();
                        intent.putExtra(COMPRESS_PATHS, new ArrayList<>());
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }

                @Override
                public void onCancel() {
                    Intent intent = new Intent();
                    intent.putExtra(COMPRESS_PATHS, new ArrayList<>());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        } else {

            int selectMimeType = SelectMimeType.ofImage();
            if("image".equals(mode)){
                selectMimeType = SelectMimeType.ofImage();
            }else if ("video".equals(mode)){
                selectMimeType = SelectMimeType.ofVideo();
            }else{
                selectMimeType = SelectMimeType.ofAll();
            }

            PictureSelector.create(this).openGallery(selectMimeType)
                    .setImageEngine(GlideEngine.createGlideEngine())
                    .setSelectorUIStyle(pictureStyleUtil.getSelectorStyle())
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    .setRecordVideoMaxSecond(videoRecordMaxSecond.intValue())
                    .setRecordVideoMinSecond(videoRecordMinSecond.intValue())
                    .setLanguage(getLang(language))
                    .setOutputCameraDir(new AppPath(this).getAppVideoDirPath())
                    .setCropEngine(enableCrop ?
                            new ImageCropEngine(this, buildOptions(selectorStyle), width.intValue(), height.intValue()) : null)
                    .setCompressEngine(new ImageCompressEngine(compressSize.intValue()))
                    .setSandboxFileEngine(new MeSandboxFileEngine())
                    .isDisplayCamera(showCamera)
                    .isGif(showGif)
                    .setPermissionsInterceptListener(new OnPermissionsInterceptListener() {
                        @Override
                        public void requestPermission(
                                Fragment fragment,
                                String[] permissions,
                                OnRequestPermissionListener call) {

                            // 这里是关键：告诉 PictureSelector
                            // 「权限已经由外层（Flutter + CustomPermissionHandler）处理好了，
                            // 不要再调用系统的 requestPermissions()」。
                            //
                            // 所以我们直接回调“全部已授权”：
                            call.onCall(permissions, true);

                            // 如果你希望在这里再做一次自定义权限检查，
                            // 也可以用 fragment.getActivity() + 自己的 PermissionUtil，
                            // 检查失败时 call.onCall(permissions, false)。
                        }
                        @Override
                        public boolean hasPermissions(Fragment fragment, String[] permissions) {
                            // 同样直接返回 true，表示权限已经具备，
                            // 这样 PictureSelector 就不会再去触发自己的权限流程。
                            return true;
                        }
                    })
                    .setSelectMaxDurationSecond(videoSelectMaxSecond.intValue())
                    .setSelectMinDurationSecond(videoSelectMinSecond.intValue())
                    // .setFilterVideoMaxSecond(videoSelectMaxSecond.intValue())
                    .setFilterVideoMinSecond(videoSelectMinSecond.intValue())
                    .setMaxSelectNum(selectCount.intValue())
                    .setMaxVideoSelectNum(selectCount.intValue())
                    .isWithSelectVideoImage(true)
                    .setImageSpanCount(4)// 每行显示个数 int
                    .setSelectionMode(selectCount.intValue() == 1 ? SelectModeConfig.SINGLE : SelectModeConfig.MULTIPLE)// 多选 or 单选 PictureConfig.MULTIPLE or PictureConfig.SINGLE
                    .isDirectReturnSingle(true)
                    .setSkipCropMimeType(new String[]{PictureMimeType.ofGIF(), PictureMimeType.ofWEBP()})
                    .isPreviewImage(true)
                    .isPreviewVideo(true)
                    .setAttachViewLifecycle(new IBridgeViewLifecycle() {
                        @Override
                        public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
                            // 在 PictureSelector 内部 Fragment 的视图创建完成后，再检测一次 Android 14 的“部分照片访问”状态，
                            // 并把提示弹窗挂在相册页面所在的 Activity 上，这样弹窗就会盖在相册 UI 上显示。

                            Log.d("getPickerPaths", "attachLifecycle onViewCreated called");

                            if (fragment == null || fragment.getContext() == null) {
                                Log.d("getPickerPaths", "attachLifecycle onViewCreated: fragment or context is null, skip");
                                return;
                            }

                            ViewGroup rootView = null;
                            if (view instanceof ViewGroup) {
                                rootView = (ViewGroup) view;
                            }

                            int sdkInt = android.os.Build.VERSION.SDK_INT;
                            int targetSdk = android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
                            Log.d("getPickerPaths", "attachLifecycle onViewCreated sdkInt=" + sdkInt + ", UPSIDE_DOWN_CAKE=" + targetSdk);

                            if (sdkInt < targetSdk) {
                                Log.d("getPickerPaths", "attachLifecycle onViewCreated: SDK < 34, skip dialog");
                                return;
                            }

                            android.content.Context context = fragment.getContext();

                            boolean hasReadImages = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_MEDIA_IMAGES
                            ) == PackageManager.PERMISSION_GRANTED;

                            boolean hasVisualSelected = false;
                            try {
                                hasVisualSelected = ContextCompat.checkSelfPermission(
                                        context,
                                        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                                ) == PackageManager.PERMISSION_GRANTED;
                            } catch (Exception e) {
                                Log.d("getPickerPaths", "attachLifecycle onViewCreated: exception when checking READ_MEDIA_VISUAL_USER_SELECTED: " + e);
                            }

                            Log.d("getPickerPaths", "attachLifecycle onViewCreated: hasReadImages=" + hasReadImages + ", hasVisualSelected=" + hasVisualSelected);

                            if (!hasReadImages && hasVisualSelected) {
                                Log.d("getPickerPaths", "attachLifecycle onViewCreated: limited photo access detected, adding top tip bar and showing custom tip dialog");

                                if (rootView != null) {
                                    // 打印当前 Fragment 的 View 树结构，后续用于精确定位提示条插入位置
                                    logViewTree(rootView, 0);

                                    View existingTip = rootView.findViewById(R.id.ps_top_tip_bar_root);
                                    if (existingTip == null) {
                                        View tipBar = LayoutInflater.from(fragment.requireActivity())
                                                .inflate(R.layout.ps_top_tip_bar, rootView, false);

                                        if (rootView instanceof ConstraintLayout) {
                                            ConstraintLayout cl = (ConstraintLayout) rootView;
                                            View recycler = cl.findViewById(R.id.recycler);
                                            View titleBar = cl.findViewById(R.id.title_bar);

                                            if (recycler != null && titleBar != null) {
                                                // 使用 XML 中定义的高度（例如 48dp/88dp），只补充约束信息
                                                ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) tipBar.getLayoutParams();
                                                lp.topToBottom = titleBar.getId();
                                                lp.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                                                lp.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
                                                tipBar.setLayoutParams(lp);
                                                cl.addView(tipBar);

                                                // 点击整块提示条，跳转到应用设置页面，并关闭相册组件，返回空结果
                                                tipBar.setOnClickListener(v -> {
                                                    // 打开应用设置页
                                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    intent.setData(Uri.fromParts("package", fragment.requireActivity().getPackageName(), null));
                                                    fragment.requireActivity().startActivity(intent);

                                                    // 关闭 PictureSelector 所在的 Activity
                                                    fragment.requireActivity().finish();

                                                    // 向 Flutter 返回一个空数组结果并关闭当前 SelectPicsActivity
                                                    Intent resultIntent = new Intent();
                                                    resultIntent.putExtra(COMPRESS_PATHS, new ArrayList<>());
                                                    setResult(RESULT_OK, resultIntent);
                                                    finish();
                                                });

                                                ViewGroup.LayoutParams recyclerLpRaw = recycler.getLayoutParams();
                                                if (recyclerLpRaw instanceof ConstraintLayout.LayoutParams) {
                                                    ConstraintLayout.LayoutParams recyclerLp = (ConstraintLayout.LayoutParams) recyclerLpRaw;
                                                    if (recyclerLp.topToBottom == titleBar.getId()) {
                                                        recyclerLp.topToBottom = tipBar.getId();
                                                        recycler.setLayoutParams(recyclerLp);
                                                    }
                                                }
                                            } else {
                                                // 兜底：未找到特定锚点时，直接添加到根布局
                                                // rootView.addView(tipBar);
                                            }
                                        } else {
                                            // 根布局不是 ConstraintLayout 时的兜底处理
                                            // rootView.addView(tipBar);
                                        }
                                    }
                                }

                                // AlertDialog dialog = new AlertDialog.Builder(fragment.requireActivity()).create();
                                // View dialogView = LayoutInflater.from(fragment.requireActivity())
                                //         .inflate(R.layout.ps_dialog_limited_photo_access, null, false);

                                // TextView tvMessage = dialogView.findViewById(R.id.tv_message);
                                // Button btnNegative = dialogView.findViewById(R.id.btn_negative);
                                // Button btnPositive = dialogView.findViewById(R.id.btn_positive);

                                // if (tvMessage != null) {
                                //     tvMessage.setText("无法访问相册中所有照片，请在系统设置中将「照片」权限改为「所有照片」");
                                // }

                                // btnNegative.setOnClickListener(v -> dialog.dismiss());
                                // btnPositive.setOnClickListener(v -> {
                                //     dialog.dismiss();
                                //     Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                //     intent.setData(Uri.fromParts("package", fragment.requireActivity().getPackageName(), null));
                                //     fragment.requireActivity().startActivity(intent);
                                // });

                                // dialog.setView(dialogView);
                                // if (dialog.getWindow() != null) {
                                //     dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                // }
                                // dialog.show();

                                // if (dialog.getWindow() != null) {
                                //     DisplayMetrics dm = new DisplayMetrics();
                                //     fragment.requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
                                //     WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                                //     params.width = (int) (dm.widthPixels * 0.7f);
                                //     dialog.getWindow().setAttributes(params);
                                // }
                            } else {
                                Log.d("getPickerPaths", "attachLifecycle onViewCreated: condition NOT matched, no dialog. hasReadImages=" + hasReadImages + ", hasVisualSelected=" + hasVisualSelected);
                            }
                        }

                        @Override
                        public void onDestroy(Fragment fragment) {
                            // 这里目前不需要额外处理，仅保留方法以满足接口要求。
                            Log.d("getPickerPaths", "attachLifecycle onDestroy called");
                        }
                    })
                    .forResult(new OnResultCallbackListener<LocalMedia>() {
                        @Override
                        public void onResult(ArrayList<LocalMedia> result) {
                            handlerResult(result);
                        }

                        @Override
                        public void onCancel() {
                            Intent intent = new Intent();
                            intent.putExtra(COMPRESS_PATHS, new ArrayList<>());
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });
        }

    }


    private View findFirstRecyclerView(ViewGroup root) {
        if (root instanceof RecyclerView) {
            return root;
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof RecyclerView) {
                return child;
            }
            if (child instanceof ViewGroup) {
                View result = findFirstRecyclerView((ViewGroup) child);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }


    private void logViewTree(View view, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("--");
        }
        sb.append(view.getClass().getSimpleName());

        int id = view.getId();
        if (id != View.NO_ID) {
            try {
                String name = view.getResources().getResourceEntryName(id);
                sb.append(" id=").append(name);
            } catch (Exception ignore) {
            }
        }

        Log.d("getPickerPaths", "viewTree: " + sb.toString());

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                logViewTree(vg.getChildAt(i), depth + 1);
            }
        }
    }


    private void handlerResult(ArrayList<LocalMedia> selectList) {
        List<Map<String, String>> paths = new ArrayList<>();
        for (int i = 0; i < selectList.size(); i++) {
            LocalMedia localMedia = selectList.get(i);

            if (localMedia.getMimeType().contains("image")){
                String path = localMedia.getAvailablePath();
                if (localMedia.isCut()) {
                    path = localMedia.getCutPath();
                }
                Map<String, String> map = new HashMap<>();
                map.put("thumbPath", path);
                map.put("path", path);
                paths.add(map);

            }else{
                if (localMedia.getAvailablePath() == null) {
                    break;
                }
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(localMedia.getAvailablePath(), MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                String thumbPath = CommonUtils.saveBitmap(this, new AppPath(this).getAppImgDirPath(), bitmap);
                Map<String, String> map = new HashMap<>();
                map.put("thumbPath", thumbPath);
                map.put("path", localMedia.getAvailablePath());
                paths.add(map);
            }

        }
        Intent intent = new Intent();
        intent.putExtra(COMPRESS_PATHS, (Serializable) paths);
        setResult(RESULT_OK, intent);
        finish();
    }

}
