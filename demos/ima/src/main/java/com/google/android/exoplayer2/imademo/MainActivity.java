/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.imademo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.exoplayer2.ExoPlayer;

/**
 * Main Activity for the IMA plugin demo. {@link ExoPlayer} objects are created by {@link
 * PlayerManager}, which this class instantiates.
 */
public final class MainActivity extends AppCompatActivity {

  //读写权限
  private static String[] PERMISSIONS_STORAGE = {
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE};
  //请求状态码
  private static int REQUEST_PERMISSION_CODE = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
    setContentView(R.layout.my_main_activity);
    initMyAct();
  }

  private void initMyAct() {
    Button button = findViewById(R.id.run_player);
    button.setOnClickListener(listener);
  }

  View.OnClickListener listener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent();
      switch (v.getId()) {
        case R.id.run_player:
          intent.setClass(MainActivity.this, PlayerMainActivity.class);
          startActivity(intent);
          break;
        default:
          break;
      }
    }
  };
}
