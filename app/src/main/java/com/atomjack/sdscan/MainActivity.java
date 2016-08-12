package com.atomjack.sdscan;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
  ScanService scanService;
  boolean bound = false;
  private boolean shouldBeginScan = false;
  private static final int PERMISSION_CODE = 1;
  private boolean resultsShowing = false;
  private Handler handler = new Handler();
  @Bind(R.id.progressNum)
  TextView progressNum;
  @Bind(R.id.scanningLayout)
  RelativeLayout scanningLayout;
  @Bind(R.id.scanResultsLayout)
  LinearLayout scanResultsLayout;

  @Bind(R.id.biggestFilesListView)
  ListView biggestFilesListView;

  @Bind(R.id.averageFileSizeView)
  TextView averageFileSizeView;

  @Bind(R.id.extensionsListView)
  ListView extensionsListView;

  private MenuItem shareButton;
  private ShareActionProvider shareActionProvider;


  // Results will be stored here. Needed to repopulate results when orientation changes
  ArrayList<File> biggestFiles;
  int averageFileSize;
  ArrayList<Map<String, Integer>> topFiveExtensions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Logger.d("onCreate");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    if(savedInstanceState != null) {
      resultsShowing = savedInstanceState.getBoolean("resultsShowing");
      if(resultsShowing) {
        scanningLayout.setVisibility(View.GONE);
        scanResultsLayout.setVisibility(View.VISIBLE);
        biggestFiles = (ArrayList<File>)savedInstanceState.getSerializable("biggestFiles");
        averageFileSize = savedInstanceState.getInt("averageFileSize");
        topFiveExtensions = (ArrayList<Map<String, Integer>>)savedInstanceState.getSerializable("topFiveExtensions");
        showScanResults(biggestFiles, averageFileSize, topFiveExtensions);
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("resultsShowing", resultsShowing);
    outState.putSerializable("biggestFiles", biggestFiles);
    outState.putInt("averageFileSize", averageFileSize);
    outState.putSerializable("topFiveExtensions", topFiveExtensions);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.toolbar_share, menu);
    shareButton = menu.getItem(0);
    shareButton.setVisible(false);
    shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareButton);
    return true;
  }

  // Call to update the share intent
  private void setShareIntent(Intent shareIntent) {
    if (shareActionProvider != null) {
      shareActionProvider.setShareIntent(shareIntent);
    }
  }

  @OnClick(R.id.scan_button)
  public void beginScan() {
    Logger.d("Beginning scan");
    if(!bound) {
      Intent intent = new Intent(this, ScanService.class);
      bindService(intent, serviceConnection, BIND_AUTO_CREATE);
      startService(intent);
      shouldBeginScan = true;
    } else {
      if(checkPermissions()) {
        scanService.beginScan();
        scanningLayout.setVisibility(View.VISIBLE);
        scanResultsLayout.setVisibility(View.GONE);
        shareButton.setVisible(false);
        resultsShowing = false;
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
        scanningLayout.setVisibility(View.VISIBLE);
        scanResultsLayout.setVisibility(View.GONE);
        resultsShowing = false;
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
        public void onFinished(final List<File> biggestFiles, final int averageFileSize, final List<Map<String, Integer>> topFiveExtensions) {
          handler.post(new Runnable() {
            @Override
            public void run() {
              // Save the results so that they can be reloaded on orientation change
              MainActivity.this.biggestFiles = (ArrayList<File>)biggestFiles;
              MainActivity.this.averageFileSize = averageFileSize;
              MainActivity.this.topFiveExtensions = (ArrayList<Map<String, Integer>>)topFiveExtensions;
              // This is where the magic happens!
              scanningLayout.setVisibility(View.GONE);
              scanResultsLayout.setVisibility(View.VISIBLE);
              shareButton.setVisible(true);
              resultsShowing = true;

              showScanResults(biggestFiles, averageFileSize, topFiveExtensions);
            }
          });
        }

        @Override
        public void onStopped() {
          handler.post(new Runnable() {
            @Override
            public void run() {
              scanningLayout.setVisibility(View.GONE);
            }
          });
        }
      });
      if(shouldBeginScan) {
        shouldBeginScan = false;
        scanService.beginScan();
        handler.post(new Runnable() {
          @Override
          public void run() {
            scanningLayout.setVisibility(View.VISIBLE);
            scanResultsLayout.setVisibility(View.GONE);
            resultsShowing = false;
          }
        });
      }
      if(scanService.isScanning()) {
        scanningLayout.setVisibility(View.VISIBLE);
        scanResultsLayout.setVisibility(View.GONE);
        resultsShowing = false;
      }

      bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      bound = false;
    }
  };

  private void showScanResults(final List<File> biggestFiles, final int averageFileSize, final List<Map<String, Integer>> topFiveExtensions) {
    // First, set the intent that will be sent when the share button is pressed
    Intent shareIntent = new Intent();
    shareIntent.setAction(Intent.ACTION_SEND);
    StringBuilder sb = new StringBuilder();
    sb.append("SD Scan Results:\n\n");
    sb.append(String.format("Average File Size: %s\n\n", Utils.humanReadableByteCount(averageFileSize)));
    sb.append("10 Biggest Files:\n");
    for(File file : biggestFiles) {
      sb.append(String.format("    %s (%s)\n", file.getName(), Utils.humanReadableByteCount(file.length())));
    }
    sb.append("\n5 Most Frequent Extensions:\n");
    for(Map<String, Integer> extension : topFiveExtensions) {
      for(String key : extension.keySet()) {
        sb.append(String.format("    %s (%d)\n", key, extension.get(key)));
      }
    }
    shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
    shareIntent.setType("text/plain");
    setShareIntent(shareIntent);


    averageFileSizeView.setText(Utils.humanReadableByteCount(averageFileSize, true));

    FileArrayAdapter biggestFilesAdapter = new FileArrayAdapter(MainActivity.this);
    biggestFilesAdapter.setFiles(biggestFiles);

    biggestFilesListView.setAdapter(biggestFilesAdapter);
    biggestFilesAdapter.notifyDataSetChanged();
    ListUtils.setDynamicHeight(biggestFilesListView);

    String[] extensionsArray = new String[topFiveExtensions.size()];
    int i = 0;
    for(Map<String, Integer> map : topFiveExtensions) {
      for (String key : map.keySet()) {
        extensionsArray[i] = String.format("%s (%d)", key, map.get(key));
        i++;
      }
    }
    ArrayAdapter<String> extensionsAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.extension_list_item, extensionsArray);
    extensionsListView.setAdapter(extensionsAdapter);
    extensionsAdapter.notifyDataSetChanged();
    ListUtils.setDynamicHeight(extensionsListView);
  }

  private void setProgressNum(final int progress) {
    progressNum.setText(Integer.toString(progress));
  }

  private boolean checkPermissions() {
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
      return false;
    }
    return true;
  }

  @OnClick(R.id.progressStopButton)
  public void onStopClicked() {
    Logger.d("activity stop clicked");
    scanService.stopScan();
  }

  @Override
  public void onBackPressed() {
    if(bound) {
      if(scanService.isScanning())
        scanService.stopScan();
    }
    super.onBackPressed();
  }

  public static class ListUtils {
    public static void setDynamicHeight(ListView mListView) {
      ListAdapter mListAdapter = mListView.getAdapter();
      if (mListAdapter == null) {
        // when adapter is null
        return;
      }
      int height = 0;
      int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
      for (int i = 0; i < mListAdapter.getCount(); i++) {
        View listItem = mListAdapter.getView(i, null, mListView);
        listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
        height += listItem.getMeasuredHeight();
      }
      ViewGroup.LayoutParams params = mListView.getLayoutParams();
      params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
      mListView.setLayoutParams(params);
      mListView.requestLayout();
    }
  }

}
