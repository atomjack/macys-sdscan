package com.atomjack.sdscan;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class ScanService extends Service {
  private final IBinder binder = new ScanBinder();
  private ScanListener scanListener;
  private int filesScanned = 0;

  private List<File> biggestFiles = new ArrayList<>();
  private int totalFileSize = 0;
  private SortedMap<String, Integer> fileExtensions = new TreeMap<>();
  private SortedSet<Map.Entry<String, Integer>> sortedExtensionSet;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  public class ScanBinder extends Binder {
    ScanService getService() {
      return ScanService.this;
    }

    public void setListener(ScanListener listener) {
      scanListener = listener;
    }
  }

  public void beginScan() {
    filesScanned = 0;
    biggestFiles.clear();
    totalFileSize = 0;
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        Filewalker fw = new Filewalker();
        fw.walk(Environment.getExternalStorageDirectory());

        // Done scanning the sdcard. We now have an array of the top 10 biggest files, the total number of files scanned,
        // the total size of all files scanned, and a list of all the extensions and their frequencies.

        // Sort the list of extensions and remove all but the top 5
        if(sortedExtensionSet == null) {
          sortedExtensionSet = new TreeSet<>(
                  new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> t0, Map.Entry<String, Integer> t1) {
                      if(t0.getValue() > t1.getValue())
                        return -1;
                      if(t0.getValue() < t1.getValue())
                        return 1;
                      return 0;
                    }
                  }
          );
        }
        sortedExtensionSet.clear();
        sortedExtensionSet.addAll(fileExtensions.entrySet());
        while(sortedExtensionSet.size() > 5) {
          sortedExtensionSet.remove(sortedExtensionSet.last());
        }

        scanListener.onFinished(biggestFiles, totalFileSize / filesScanned, sortedExtensionSet);
        return null;
      }
    }.execute();
  }

  class Filewalker {
    public void walk(File root) {
      File[] list = root.listFiles();

      for(File f : list) {
        if(f.isDirectory()) {
          walk(f);
        } else {
          // Add the file's size to the total
          totalFileSize += f.length();

          // Add one to the frequency of this file's extension, but only if the filename contains a .
          if(f.getName().contains(".")) {
            String extension = f.getName().substring(f.getName().lastIndexOf(".") + 1, f.getName().length());
            if (!extension.equals("")) {
              if (fileExtensions.get(extension) == null) {
                fileExtensions.put(extension, 1);
              } else
                fileExtensions.put(extension, fileExtensions.get(extension) + 1);
            }
          }


          if(biggestFiles.size() < 10) // If we have less than 10 files, add this new one
            biggestFiles.add(f);
          else {
            if(biggestFiles.get(biggestFiles.size() - 1).length() < f.length()) {
              // The file we are adding is bigger than the smallest file in the array
              // So, add it to the array, sort, and then trim the array to the first 10 entries
              biggestFiles.add(f);
              File[] files = biggestFiles.toArray(new File[0]);
              // Now sort the array, largest first
              Arrays.sort(files, FileSizeComparator);
              biggestFiles = new ArrayList<>(Arrays.asList(files));

              // Trim the array down to the top 10 biggest files
              if(biggestFiles.size() > 10) {
                biggestFiles.subList(10, biggestFiles.size()).clear();
              }
            }
          }

          filesScanned++;
          if(filesScanned % 10 == 0) { // Alert the main activity of the total number of files scanned, every 10 files
            scanListener.onProgressUpdate(filesScanned);
          }
        }
      }
    }
  }

  private Comparator<File> FileSizeComparator = new Comparator<File>() {
    @Override
    public int compare(File file, File t1) {
      if(file.length() > t1.length())
        return -1;
      else if(file.length() < t1.length())
        return 1;
      return 0;
    }
  };
}
