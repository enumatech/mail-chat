package io.enuma.app.keystoretest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by llunesu on 1/1/2017.
 */

public abstract class BaseActivity extends AppCompatActivity {

    private static int activeCount = 0;


    @Override
    protected void onResume() {

        Intent intent = new Intent(this, ImapService.class);
        startService(intent);

        activeCount++;

        super.onResume();
    }


    @Override
    protected void onPause() {

        activeCount--;

        super.onPause();
    }


    @Override
    protected void onStop() {

        if (activeCount == 0) {
            Intent intent = new Intent(this, ImapService.class);
            stopService(intent);
        }

        super.onStop();
    }

}
