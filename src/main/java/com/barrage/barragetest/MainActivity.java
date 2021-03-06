package com.barrage.barragetest;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import com.barrage.barragetest.activity.PlayVideoActivity;
import com.barrage.barragetest.manager.PermissionsManager;

public class MainActivity extends AppCompatActivity {

    private static int SETTING_PERMISSION = 100;
    private PermissionsManager permissionsManager;
    private AppCompatButton bt_play;
    private MaterialButton bt_float;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayout;
    // 窗口宽高值
    private float x, y;
    //悬浮窗口布局
    private View mWindowsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        bt_play = findViewById(R.id.bt_play);
        bt_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionsManager.checkReadPermission(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, PlayVideoActivity.class));
                }
            }
        });

        bt_float = findViewById(R.id.bt_float);
        bt_float.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showWindow();
            }
        });
    }

    //显示悬浮窗口
    public void showWindow() {
        //先检查是否具有悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        } else {
            // 取得系统窗体
            mWindowManager = (WindowManager) getApplicationContext()
                    .getSystemService(WINDOW_SERVICE);
            // 窗体的布局样式
            mLayout = new WindowManager.LayoutParams();
            // 设置窗体显示类型——TYPE_SYSTEM_ALERT(系统提示)
            mLayout.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            // 设置窗体焦点及触摸：
            // FLAG_NOT_FOCUSABLE(不能获得按键输入焦点)
            mLayout.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            // 设置显示的模式
            mLayout.format = PixelFormat.RGBA_8888;
            // 设置对齐的方法
            mLayout.gravity = Gravity.TOP | Gravity.LEFT;
            // 设置窗体宽度和高度
            // 设置视频的播放窗口大小
            mLayout.width = 700;
            mLayout.height = 400;
            mLayout.x = 300;
            mLayout.y = 300;
            //将指定View解析后添加到窗口管理器里面
            mWindowsView = View.inflate(this, R.layout.layout_window, null);
            VideoView vv_float_video = (VideoView) mWindowsView.findViewById(R.id.vv_float_video);
            mWindowsView.findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeWindow();
                }
            });
            playVideo(vv_float_video);
            mWindowManager.addView(mWindowsView, mLayout);
            mWindowsView.setOnTouchListener(new View.OnTouchListener() {
                float mTouchStartX;
                float mTouchStartY;
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    x = event.getRawX();
                    y = event.getRawY() - 25;//25状态栏大小
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mTouchStartX = event.getX();
                            mTouchStartY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            //原始坐标减去移动坐标
                            mLayout.x = (int) (x - mTouchStartX);
                            mLayout.y = (int) (y - mTouchStartY);
                            mWindowManager.updateViewLayout(mWindowsView, mLayout);
                            Log.i("main", "x值=" + x + "\ny值=" + y + "\nmTouchX" + mTouchStartX + "\nmTouchY=" + mTouchStartY);
                            break;
                    }
                    return true;
                }
            });
        }
    }

    //播放视频
    private void playVideo(VideoView videoView) {
        //获取本地视频文件进行播放
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (c.moveToNext()) {
            String path = c.getString(c.getColumnIndex(MediaStore.Video.Media.DATA));
            videoView.setVideoPath(path);
            videoView.requestFocus();
            videoView.start();
        }
    }

    //关闭窗口点击事件
    public void closeWindow() {
        mWindowManager.removeView(mWindowsView);
    }

    //申请权限
    private void requestPermission() {
        // 当API大于 23 时，才动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           PermissionsManager.requestReadPermission(this);
           PermissionsManager.requestFloatPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionsManager.SHOW_REQUEST_CODE:
                //权限请求失败
                if (grantResults.length == PermissionsManager.SHOW_REQUEST.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            showDialog();
                            Toast.makeText(MainActivity.this, "请求读写权限被拒绝", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                }
            case PermissionsManager.FLOAT_REQUEST_CODE:
                //权限请求失败
                if (grantResults.length == PermissionsManager.SHOW_REQUEST.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            showDialog();
                            Toast.makeText(MainActivity.this, "请求悬浮窗权限被拒绝", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                }
                break;
        }
    }

    //弹出提示框
    private void showDialog(){
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("播放视频需要读写权限，是否去设置？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        goToAppSetting();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, SETTING_PERMISSION);
    }
}

