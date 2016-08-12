package com.atomjack.sdscan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileArrayAdapter extends BaseAdapter {
  private List<File> files = new ArrayList<>();
  private Context context;

  public FileArrayAdapter(Context context) {
    this.context = context;
  }

  public void setFiles(List<File> files) {
    this.files = files;
  }

  @Override
  public Object getItem(int i) {
    return files.get(i);
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View rowView = inflater.inflate(R.layout.file_list_item, viewGroup, false);
    File file = files.get(i);
    TextView fileNameView = (TextView)rowView.findViewById(R.id.fileName);
    fileNameView.setText(file.getName());
    TextView fileSizeView = (TextView)rowView.findViewById(R.id.fileSize);
    fileSizeView.setText(Utils.humanReadableByteCount(file.length(), true));
    return rowView;
  }

  @Override
  public int getCount() {
    return files.size();
  }


}
