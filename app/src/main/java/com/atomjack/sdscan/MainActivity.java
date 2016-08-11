package com.atomjack.sdscan;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
  ScanService scanService;
  boolean bound = false;
  private boolean shouldBeginScan = false;
  private static final int PERMISSION_CODE = 1;
  private Dialog progressDialog;
  private TextView progressDialogNum;
  private Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.scan_button)
  public void beginScan() {
    Logger.d("beginScan, bound: %s", bound);
    if(!bound) {
      Intent intent = new Intent(this, ScanService.class);
      bindService(intent, serviceConnection, BIND_AUTO_CREATE);
      startService(intent);
      shouldBeginScan = true;
    } else {
      if(checkPermissions()) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            createDialog();
          }
        });
        scanService.beginScan();
      } else {
        shouldBeginScan = true;
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    Logger.d("onStart");
    Intent intent = new Intent(this, ScanService.class);
    bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    startService(intent);

    checkPermissions();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if(requestCode == PERMISSION_CODE) {
      if(shouldBeginScan) {
        scanService.beginScan();
      }
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if(bound) {
      unbindService(serviceConnection);
      bound = false;
    }
  }

  private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      Logger.d("onServiceConnected");
      ScanService.ScanBinder binder = (ScanService.ScanBinder) service;
      scanService = binder.getService();
      binder.setListener(new ScanListener() {
        @Override
        public void onProgressUpdate(final int progress) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              setProgressNum(progress);
            }
          });
        }

        @Override
        public void onFinished(final List<File> biggestFiles, final int averageFileSize, final SortedSet<Map.Entry<String, Integer>> topTenExtensions) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              progressDialog.hide();
            }
          });
        }
      });
      if(shouldBeginScan) {
        shouldBeginScan = false;
        scanService.beginScan();
      }

      bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      bound = false;
    }
  };

  private void setProgressNum(final int progress) {
//    handler.post(new Runnable() {
//      @Override
//      public void run() {
    progressDialogNum.setText(Integer.toString(progress));
//      }
//    });
  }

  private void createDialog() {
    if(progressDialog != null) {
      setProgressNum(0);
      progressDialog.show();
    } else {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      LayoutInflater inflater = getLayoutInflater();
      final View layout = inflater.inflate(R.layout.progress_dialog, null);
      progressDialogNum = (TextView) layout.findViewById(R.id.progressDialogNum);
      builder.setView(layout);
      progressDialog = builder.create();
      progressDialog.show();
    }
  }

  private boolean checkPermissions() {
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
      return false;
    }
    return true;
  }
}
