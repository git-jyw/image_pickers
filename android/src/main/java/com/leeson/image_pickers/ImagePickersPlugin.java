package com.leeson.image_pickers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.leeson.image_pickers.activitys.PermissionActivity;
import com.leeson.image_pickers.activitys.PhotosActivity;
import com.leeson.image_pickers.activitys.SelectPicsActivity;
import com.leeson.image_pickers.activitys.VideoActivity;
import com.leeson.image_pickers.utils.Saver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import android.util.Log;

/**
 * Created by lisen on 2019/10/16.
 *
 * @author lisen < 453354858@qq.com >
 */
@SuppressWarnings("all")
public class ImagePickersPlugin implements FlutterPlugin,MethodChannel.MethodCallHandler, ActivityAware {

  private MethodChannel channel;

  private static final int SELECT = 102;
  private static final int SAVE_IMAGE = 103;
  private static final int SAVE_VIDEO = 104;
  private static final int SAVE_IMAGE_DATA = 105;
  private static final int READ_IMAGE = 106;

  private Activity activity;
  private MethodChannel.Result result;

  private byte[] data;

  // 记录是否已经通过 PermissionActivity 成功申请过一次媒体读取权限，
  // 用于避免在 Android 13/14 的“部分照片访问”场景下重复弹出系统的“选择项目”界面。
  private static boolean hasRequestedMediaPermissionOnce = false;

  public ImagePickersPlugin() {
  }


  private PluginRegistry.ActivityResultListener listener = new PluginRegistry.ActivityResultListener() {
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
      Log.d("getPickerPaths", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
      if (requestCode == SELECT ) {
        if (resultCode == Activity.RESULT_OK){
          List<Map<String,String>> paths = (List<Map<String,String>>) intent.getSerializableExtra(SelectPicsActivity.COMPRESS_PATHS);
          if (result != null){
            result.success(paths);
          }
        }else{
          if (result != null){
            result.success(new ArrayList<>());
          }
        }
        return true;
      }else if (requestCode == SAVE_IMAGE){
        if (resultCode == Activity.RESULT_OK){
          String imageUrl = intent.getStringExtra("imageUrl");
          saveImage(imageUrl);
        }
      }else if(requestCode == SAVE_VIDEO){
        if (resultCode == Activity.RESULT_OK){
          String videoUrl = intent.getStringExtra("videoUrl");
          saveVideo(videoUrl);
        }
      }else if(requestCode == SAVE_IMAGE_DATA){
        if (resultCode == Activity.RESULT_OK && data != null){
          saveImageData();
        }
      }else if (requestCode == READ_IMAGE){
        if (resultCode == Activity.RESULT_OK){
          Log.d("getPickerPaths", "onActivityResult: READ_IMAGE RESULT_OK, set hasRequestedMediaPermissionOnce=true");
          hasRequestedMediaPermissionOnce = true;

          Intent intent1 = new Intent(activity, SelectPicsActivity.class);
          intent1.putExtras(intent);
          activity.startActivityForResult(intent1, SELECT);
        } else {
          Log.d("getPickerPaths", "onActivityResult: READ_IMAGE result not OK, do not update hasRequestedMediaPermissionOnce");
        }

      }
      return false;
    }
  };

  @Override
  public void onMethodCall(MethodCall methodCall, @NonNull MethodChannel.Result result) {

    this.result = result;
    if ("getPickerPaths".equals(methodCall.method)) {
      Log.d("getPickerPaths", "onMethodCall: getPickerPaths");
      String galleryMode = methodCall.argument("galleryMode");
      Boolean showGif = methodCall.argument("showGif");
      Map<String,Number> uiColor = methodCall.argument("uiColor");
      Number selectCount = methodCall.argument("selectCount");
      Boolean showCamera = methodCall.argument("showCamera");
      Boolean enableCrop = methodCall.argument("enableCrop");
      Number width = methodCall.argument("width");
      Number height = methodCall.argument("height");
      Number compressSize = methodCall.argument("compressSize");
      String cameraMimeType = methodCall.argument("cameraMimeType");
      Number videoRecordMaxSecond = methodCall.argument("videoRecordMaxSecond");
      Number videoRecordMinSecond = methodCall.argument("videoRecordMinSecond");
      Number videoSelectMaxSecond = methodCall.argument("videoSelectMaxSecond");
      Number videoSelectMinSecond = methodCall.argument("videoSelectMinSecond");
      String language = methodCall.argument("language");

      Intent intent = new Intent();

      intent.putExtra(SelectPicsActivity.GALLERY_MODE,galleryMode);
      intent.putExtra(SelectPicsActivity.UI_COLOR, (Serializable) uiColor);
      intent.putExtra(SelectPicsActivity.SELECT_COUNT,selectCount);
      intent.putExtra(SelectPicsActivity.SHOW_GIF,showGif);
      intent.putExtra(SelectPicsActivity.SHOW_CAMERA,showCamera);
      intent.putExtra(SelectPicsActivity.ENABLE_CROP,enableCrop);
      intent.putExtra(SelectPicsActivity.WIDTH,width);
      intent.putExtra(SelectPicsActivity.HEIGHT,height);
      intent.putExtra(SelectPicsActivity.COMPRESS_SIZE,compressSize);
      //直接调用拍照或拍视频时有效
      intent.putExtra(SelectPicsActivity.CAMERA_MIME_TYPE,cameraMimeType);
      intent.putExtra(SelectPicsActivity.VIDEO_RECORD_MAX_SECOND,videoRecordMaxSecond);
      intent.putExtra(SelectPicsActivity.VIDEO_RECORD_MIN_SECOND,videoRecordMinSecond);
      intent.putExtra(SelectPicsActivity.VIDEO_SELECT_MAX_SECOND,videoSelectMaxSecond);
      intent.putExtra(SelectPicsActivity.VIDEO_SELECT_MIN_SECOND,videoSelectMinSecond);
      intent.putExtra(SelectPicsActivity.LANGUAGE,language);
      if(cameraMimeType != null){
        //为什么这么写？  PictureSelector中 有bug，在无任何权限情况下首次直接调用打开相机，会出现一个透明的activity
        Log.d("getPickerPaths", "getPickerPaths: cameraMimeType != null, start PermissionActivity for CAMERA only");
        intent.putExtra(PermissionActivity.PERMISSIONS, new String[]{Manifest.permission.CAMERA});
        intent.setClass(activity,PermissionActivity.class);
        activity.startActivityForResult(intent,READ_IMAGE);
      }else{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
          Log.d("getPickerPaths", "getPickerPaths: sdk=" + Build.VERSION.SDK_INT + " (>=33), checking media permissions");
          // Android 13+ 先判断是否已经具备任意形式的媒体访问权限（全部或部分）。
          int readImagesGrant = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES);
          int readVideoGrant = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO);
          boolean hasAnyMediaPermission =
                  readImagesGrant == PackageManager.PERMISSION_GRANTED
                          || readVideoGrant == PackageManager.PERMISSION_GRANTED;

          Log.d("getPickerPaths", "getPickerPaths: READ_MEDIA_IMAGES=" + readImagesGrant + ", READ_MEDIA_VIDEO=" + readVideoGrant);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 额外检查“仅选定照片/视频访问”权限。
            int visualSelectedGrant = ContextCompat.checkSelfPermission(activity, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
            Log.d("getPickerPaths", "getPickerPaths: READ_MEDIA_VISUAL_USER_SELECTED=" + visualSelectedGrant);
            hasAnyMediaPermission = hasAnyMediaPermission
                    || visualSelectedGrant == PackageManager.PERMISSION_GRANTED;
          }

          Log.d("getPickerPaths", "getPickerPaths: hasAnyMediaPermission=" + hasAnyMediaPermission + ", hasRequestedMediaPermissionOnce=" + hasRequestedMediaPermissionOnce);

          if (hasAnyMediaPermission || hasRequestedMediaPermissionOnce) {
            // 已经有媒体访问权限（全部或部分），或者已经通过 PermissionActivity 成功申请过一次，
            // 直接进入选择页面，避免重复弹系统“选择项目”。
            Log.d("getPickerPaths", "getPickerPaths: go SelectPicsActivity directly (skip PermissionActivity)");
            intent.setClass(activity,SelectPicsActivity.class);
            activity.startActivityForResult(intent, SELECT);
          } else {
            // 尚未授权任何媒体权限，走 PermissionActivity 触发系统权限弹窗。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
              Log.d("getPickerPaths", "getPickerPaths: no media permission yet, sdk>=34, start PermissionActivity with partial access support");
              intent.putExtra(PermissionActivity.PERMISSIONS, new String[]{
                      Manifest.permission.READ_MEDIA_IMAGES,
                      Manifest.permission.READ_MEDIA_VIDEO,
                      "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
              });
            } else {
              Log.d("getPickerPaths", "getPickerPaths: no media permission yet, sdk=33, start PermissionActivity for READ_MEDIA_IMAGES/VIDEO");
              intent.putExtra(PermissionActivity.PERMISSIONS, new String[]{
                      Manifest.permission.READ_MEDIA_IMAGES,
                      Manifest.permission.READ_MEDIA_VIDEO
              });
            }
            Log.d("getPickerPaths", "getPickerPaths: start PermissionActivity (READ_IMAGE)");
            intent.setClass(activity,PermissionActivity.class);
            activity.startActivityForResult(intent,READ_IMAGE);
          }
        }else{
          Log.d("getPickerPaths", "getPickerPaths: sdk<33, go SelectPicsActivity without media permission check");
          intent.setClass(activity,SelectPicsActivity.class);
          activity.startActivityForResult(intent, SELECT);
        }
      }

    } else if ("previewImage".equals(methodCall.method)) {
      Intent intent = new Intent(activity, PhotosActivity.class);
      List<String> images = new ArrayList<>();
      images.add(methodCall.argument("path").toString());
      intent.putExtra(PhotosActivity.IMAGES, (Serializable) images);
      activity.startActivity(intent);
    } else if ("previewImages".equals(methodCall.method)) {
      Intent intent = new Intent(activity, PhotosActivity.class);
      List<String> images = methodCall.argument("paths");
      Number initIndex = methodCall.argument("initIndex");
      intent.putExtra(PhotosActivity.IMAGES, (Serializable) images);
      intent.putExtra(PhotosActivity.CURRENT_POSITION, initIndex);
      activity.startActivity(intent);
    } else if ("previewVideo".equals(methodCall.method)) {
      Intent intent = new Intent(activity, VideoActivity.class);
      intent.putExtra(VideoActivity.VIDEO_PATH, methodCall.argument("path").toString());
      intent.putExtra(VideoActivity.THUMB_PATH, methodCall.argument("thumbPath").toString());
      activity.startActivity(intent);
    } else if("saveImageToGallery".equals(methodCall.method)) {
      String imageUrl = methodCall.argument("path").toString();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        saveImage(imageUrl);
      }else{
        Intent intent = new Intent(activity, PermissionActivity.class);
        intent.putExtra(PermissionActivity.PERMISSIONS, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        intent.putExtra("imageUrl",imageUrl);
        activity.startActivityForResult(intent,SAVE_IMAGE);
      }

    } else if("saveVideoToGallery".equals(methodCall.method)) {
      String videoUrl = methodCall.argument("path").toString();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        saveVideo(videoUrl);
      }else{
        Intent intent = new Intent(activity, PermissionActivity.class);
        intent.putExtra(PermissionActivity.PERMISSIONS, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        intent.putExtra("videoUrl",videoUrl);
        activity.startActivityForResult(intent, SAVE_VIDEO);
      }
    } else if("saveByteDataImageToGallery".equals(methodCall.method)){
      data = (byte[])methodCall.argument("uint8List");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        saveImageData();
      }else{
        Intent intent = new Intent(activity, PermissionActivity.class);
        intent.putExtra(PermissionActivity.PERMISSIONS, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
        activity.startActivityForResult(intent, SAVE_IMAGE_DATA);
      }
    }else {
      result.notImplemented();
    }
  }

  private void saveImage(String imageUrl){
    Saver imageSaver = new Saver(activity);
    imageSaver.saveImgToGallery(imageUrl, new Saver.IFinishListener() {
      @Override
      public void onSuccess(Saver.FileInfo fileInfo) {
        if (result != null){
          result.success(fileInfo.getPath());
        }
      }

      @Override
      public void onFailed(String errorMsg) {
        if (result != null){
          result.error("-1",errorMsg,errorMsg);
        }
      }
    });
  }
  private void saveVideo(String videoUrl){
    Saver videoSaver = new Saver(activity);
    videoSaver.saveVideoToGallery(videoUrl, new Saver.IFinishListener() {
      @Override
      public void onSuccess(Saver.FileInfo fileInfo) {
        if (result != null){
          result.success(fileInfo.getPath());
        }
      }

      @Override
      public void onFailed(String errorMsg) {
        if (result != null){
          result.error("-1",errorMsg,errorMsg);
        }
      }
    });
  }
  private void saveImageData(){
    Saver saver = new Saver(activity);
    saver.saveByteDataToGallery(data, new Saver.IFinishListener() {
      @Override
      public void onSuccess(Saver.FileInfo fileInfo) {
        if (result != null){
          result.success(fileInfo.getPath());
        }
        data = null;
      }

      @Override
      public void onFailed(String errorMsg) {
        if (result != null){
          result.error("-1",errorMsg,errorMsg);
        }
        data = null;
      }
    });
  }
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter/image_pickers");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    // 听网友建议 添加了判断
    if (channel != null){
      channel.setMethodCallHandler(null);
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener(listener);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
}
