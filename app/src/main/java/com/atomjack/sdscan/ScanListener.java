package com.atomjack.sdscan;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface ScanListener {
  void onProgressUpdate(int progress);
  void onFinished(List<File> biggestFiles, int averageFileSize, SortedSet<Map.Entry<String, Integer>> topTenExtensions);
}
