package com.atomjack.sdscan;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ScanListener {
  void onProgressUpdate(int progress);
  void onFinished(List<File> biggestFiles, int averageFileSize, List<Map<String, Integer>> topFiveExtensions);
  void onStopped();
}
