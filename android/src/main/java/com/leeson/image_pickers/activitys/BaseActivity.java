package com.leeson.image_pickers.activitys;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


/**
 * Created by lisen on 2018/4/12.
 *
 * @author lisen < 453354858@qq.com >
 */
@SuppressWarnings("all")
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "getPickerPaths";

    private int REQUEST_CODE_PERMISSION = 0x00001;


    /**
     * 请求权限
     *
     * @param permissions 请求的权限
     * @param requestCode 请求权限的请求码
     */
    public void requestPermission(String[] permissions, int requestCode) {
        this.REQUEST_CODE_PERMISSION = requestCode;
        if (checkPermissions(permissions)) {
            permissionSuccess(REQUEST_CODE_PERMISSION);
        } else {
            List<String> needPermissions = getDeniedPermissions(permissions);
            ActivityCompat.requestPermissions(this, needPermissions.toArray(new String[needPermissions.size()]), REQUEST_CODE_PERMISSION);
        }
    }
    /**
     * 检测所有的权限是否都已授权
     *
     * @param permissions
     * @return
     */
    private boolean checkPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "checkPermissions: sdk<23, treat as granted");
            return true;
        }
        for (String permission : permissions) {
            int grant = ContextCompat.checkSelfPermission(this, permission);
            Log.d(TAG, "checkPermissions: permission=" + permission + ", grant=" + grant + ", sdk=" + Build.VERSION.SDK_INT);
            if (grant != PackageManager.PERMISSION_GRANTED) {
                // Android 14+ 部分照片/视频访问兼容：
                // 如果应用已经拥有 READ_MEDIA_VISUAL_USER_SELECTED 权限，
                // 即便 READ_MEDIA_IMAGES/READ_MEDIA_VIDEO 自身未完全授权，也视为已具备访问选定媒体的能力。
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        && (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)
                        || Manifest.permission.READ_MEDIA_VIDEO.equals(permission))) {
                    int visualSelectedGrant = ContextCompat.checkSelfPermission(
                            this,
                            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                    );
                    Log.d(TAG, "checkPermissions: visual_selected grant=" + visualSelectedGrant);
                    if (visualSelectedGrant == PackageManager.PERMISSION_GRANTED) {
                        // 将“仅选定照片/视频访问”视为已授权，继续检查其它权限。
                        Log.d(TAG, "checkPermissions: treat partial access as granted for " + permission);
                        continue;
                    }
                }
                Log.d(TAG, "checkPermissions: deny because permission not granted and no partial override: " + permission);
                return false;
            }
        }
        Log.d(TAG, "checkPermissions: all granted");
        return true;
    }

    /**
     * 获取权限集中需要申请权限的列表
     *
     * @param permissions
     * @return
     */
    private List<String> getDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                needRequestPermissionList.add(permission);
            }
        }
        return needRequestPermissionList;
    }


    /**
     * 系统请求权限回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            // 这里改为再次基于当前权限状态进行判断，
            // 以便在 Android 14+ 上正确识别“部分照片/视频访问”场景。
            boolean ok = checkPermissions(permissions);
            Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode + ", ok=" + ok);
            if (ok) {
                permissionSuccess(REQUEST_CODE_PERMISSION);
            } else {
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
//                        当用户设置不在询问，并且勾选拒绝权限后，显示提示对话框
                        permissonNecessity(REQUEST_CODE_PERMISSION);
                        return;
                    }
                }
                permissionFail(REQUEST_CODE_PERMISSION);
            }
        }
    }

    /**
     * 确认所有的权限是否都已授权
     *
     * @param grantResults
     * @return
     */
    private boolean verifyPermissions(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void showSettingDialog(){
        showTipsDialog();
    }

    /**
     * 当用户设置不在询问，并且勾选拒绝权限后，显示提示对话框
     */
    public void showTipsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示信息")
                .setCancelable(false)
                .setMessage("当前应用缺少必要权限，该功能暂时无法使用。如若需要，请单击【确定】按钮前往设置中心进行权限授权。")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startAppSettings();
                        finish();
                    }
                }).show();
    }

    /**
     * 启动当前应用设置页面
     */
    public void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    /**
     * 获取权限成功 子类调用
     *
     * @param requestCode
     */
    public void permissionSuccess(int requestCode) {

    }

    /**
     * 权限获取失败
     * @param requestCode
     */
    public void permissionFail(int requestCode) {
    }
    /**
     * 必要权限获取失败后(子类页面可以重写，做相应的操作)
     */
    public void permissonNecessity(int requestCode){

    }

}
