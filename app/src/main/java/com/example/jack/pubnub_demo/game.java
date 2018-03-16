package com.example.jack.pubnub_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class game extends AppCompatActivity {
    private PubNub pn;
    private TextView mTextMessage;
    private ImageView image;
    private boolean isGuesser; //If the this player is playing as a guesser.
    private boolean waitingForImage; //If the current message is to be interpreted as an image to be guessed

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Configures the PubNub
        PNConfiguration config = new PNConfiguration();
        config.setPublishKey(Constants.PUBLISH);
        config.setSubscribeKey(Constants.SUBSCRIBE);
        config.setUuid(Constants.username);
        config.setSecure(true);
        this.pn = new PubNub(config);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        //The first person to connect to the channel is not the guesser.
        isGuesser = pn.getSubscribedChannels().size() != 1;
        waitingForImage = true;

        //Sets up UI elements.
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        image = (ImageView) findViewById(R.id.imageView2);
    }

    public class PubSubPnCallback extends SubscribeCallback {
        @Override
        public void status(PubNub pubnub, PNStatus status) {
            // for common cases to handle, see: https://www.pubnub.com/docs/java/pubnub-java-sdk-v4
        }
        @Override
        public void message(PubNub pubnub, PNMessageResult message) {
            try {
                String messageConverted = message.getMessage().getAsString();
                if (isGuesser && waitingForImage) { //For when the opponent initially sends the image to be guessed.


                        //Then we search google for first img's url, store it
                        String imageFromMessage = getImageURL(messageConverted);

                        //Then download image and replace image with it's contents.
                        new DownloadImageTask(image).execute(imageFromMessage);
                        waitingForImage = false;
                } else { //For receiving plain text-based messages from opponent.
                    TextView messageView = findViewById(R.id.messageView);
                    messageView.setText(messageConverted);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            // no presence handling for simplicity
        }
    }

    //REVISE THIS

    /**
     * Replaces the image with the image to be guessed.
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    private String getImageURL(String image){
        String imageURL = "";
        try{
            URL url = new URL("https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=" + image);
            URLConnection connection = url.openConnection();

            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONObject json = new JSONObject(builder.toString());
            imageURL = json.getJSONObject("responseData").getJSONArray("results").getJSONObject(0).getString("url");
        } catch(Exception e){
            e.printStackTrace();
        }
        return imageURL;
    }

}
