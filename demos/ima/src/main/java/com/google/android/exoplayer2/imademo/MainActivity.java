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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;

/**
 * Main Activity for the IMA plugin demo. {@link ExoPlayer} objects are created by {@link
 * PlayerManager}, which this class instantiates.
 */
public final class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.my_main_activity);

    initMyAct();
  }

  private void initMyAct() {
    Button button = findViewById(R.id.run_player);
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
