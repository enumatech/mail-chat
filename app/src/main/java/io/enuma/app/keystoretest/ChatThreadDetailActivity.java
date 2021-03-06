package io.enuma.app.keystoretest;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import static io.enuma.app.keystoretest.ChatThreadListActivity.CREATE_REQUEST_CODE;
import static io.enuma.app.keystoretest.ChatThreadListActivity.isActive;

/**
 * An activity representing a single ChatThread detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ChatThreadListActivity}.
 */
public class ChatThreadDetailActivity extends BaseActivity {

    private final ChatThreadDetailFragment fragment = new ChatThreadDetailFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatthread_detail);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
//        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ChatThreadDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(ChatThreadDetailFragment.ARG_ITEM_ID));

            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chatthread_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, ChatThreadListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ADD_MESSAGE);
        filter.addAction(Constants.UPDATE_MESSAGE_STATUS);
        registerReceiver(fragment.receiver, filter);

        super.onResume();
    }


    @Override
    protected void onPause() {

        unregisterReceiver(fragment.receiver);

        super.onPause();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //Use Data to get string
                String string = data.getStringExtra("RESULT_STRING");
                if (string != null) {

                    // TODO: create group
                }
            }
        }
    }

}
