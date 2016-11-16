package io.enuma.app.keystoretest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import io.enuma.app.keystoretest.dummy.DummyContent;

import static io.enuma.app.keystoretest.ChatThreadListActivity.setGravatarImage;

public class ShareContactActivity extends AppCompatActivity {

    public static final String ARG_EMAIL_ADDRESS = "EMAIL_ADDRESS";
    public static final String ARG_DISPLAY_NAME = "DISPLAY_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_contact);

        String emailAddress;
        String displayName = "";

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(ARG_EMAIL_ADDRESS)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            emailAddress = bundle.getString(ARG_EMAIL_ADDRESS);
            displayName = bundle.getString(ARG_DISPLAY_NAME, "");

        } else {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            emailAddress = sharedPreferences.getString("email_address", "");
            displayName = sharedPreferences.getString("display_name", "");
        }

        TextView disp = (TextView)findViewById(R.id.share_contact_display_name);
        disp.setText(displayName);
        TextView email = (TextView)findViewById(R.id.share_contact_email_address);
        email.setText(emailAddress);

        ChatContact contact = DummyContent.ITEM_MAP.get(emailAddress);
        if (contact == null) {
            contact = new ChatContact(emailAddress);
        }
        ImageView imageView = (ImageView)findViewById(R.id.share_contact_avatar);
        setGravatarImage(imageView, contact);

        // FIXME: needs commas
        String qrData = "MECARD:N:"+displayName+";EMAIL:"+emailAddress+";;\n";
        int qrCodeDimention = 500;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimention);

        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            // ImageView to display the QR code in.  This should be defined in
            // your Activity's XML layout file
            ImageView imageView2 = (ImageView) findViewById(R.id.qrCode);
            imageView2.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }

        EditText editText = (EditText) findViewById(R.id.share_contact_email);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                setContentView(v);
                return true;
            }
        });

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
            Intent intent = NavUtils.getParentActivityIntent(this);
            navigateUpTo(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setContentView(View view) {
        EditText editText = (EditText) findViewById(R.id.share_contact_email);
        String email = editText.getText().toString().trim();
        if (email.length() > 0) {
            Intent intent = new Intent();
            intent.putExtra("RESULT_STRING", email);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
