package com.example.jack.pubnub_demo;

import android.annotation.SuppressLint;
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
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.CustomsearchRequestInitializer;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.presence.PNHereNowChannelData;
import com.pubnub.api.models.consumer.presence.PNHereNowResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class game extends AppCompatActivity {
    private PubNub pn;
    private TextView mTextMessage;
    private ImageView image;
    private View mSend;
    private boolean isGuesser; //If the this player is playing as a guesser.
    private boolean waitingForImage; //If the current message is to be interpreted as an image to be guessed
    private List<String> otherUsers;
    private PubSubPnCallback sub;


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
        otherUsers = new ArrayList<String>();

        //The first person to connect to the channel is not the guesser.
        PubSubPnCallback sub = new PubSubPnCallback();
        this.pn.addListener(sub);
        this.pn.subscribe().channels(Arrays.asList(Constants.GAME_CHANNEL)).withPresence().execute();
        isGuesser = otherUsers.size() != 1;
        waitingForImage = true;
        if (isGuesser){
            ((TextView) findViewById(R.id.messageView)).setText("You are the Guesser");
        } else {
            ((TextView) findViewById(R.id.messageView)).setText("You need to submit a word.");
        }

        //Sets up UI elements.
        mTextMessage = (TextView) findViewById(R.id.message);
        image = (ImageView) findViewById(R.id.imageView2);
        mSend = findViewById(R.id.button2);
        mSend.setOnClickListener(sendListener);
    }

    /**
     * Callback method for the PubNub.  Used to get the messages from the PubNub, update the
     * ImageView.
     *
     * Based off of (but not directly copied from) the PubNub Android tutorial.
     * https://www.pubnub.com/tutorials/android/chat-basics/
     */
    public class PubSubPnCallback extends SubscribeCallback {
        @Override
        public void status(PubNub pubnub, PNStatus status) {
            // for common cases to handle, see: https://www.pubnub.com/docs/java/pubnub-java-sdk-v4
        }
        @Override
        public void message(PubNub pubnub, PNMessageResult message) {
            try {

                String messageConverted = message.getMessage().getAsJsonObject().get("message").getAsString();
                if (isGuesser && waitingForImage) { //For when the opponent initially sends the image to be guessed.

                        //Then we search google for first img's url, store it
                        String imageFromMessage = getImage(messageConverted);
                        Log.v("URL", imageFromMessage);
                        //Then download image and replace image with it's contents.
                        new DownloadImageTask(image).execute(imageFromMessage);
                        waitingForImage = false;
                } else { //For receiving plain text-based messages from opponent.
                    TextView messageView = findViewById(R.id.oppMsg);
                    messageView.setText(messageConverted);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        public void presence(PubNub pubnub, PNPresenceEventResult presence) {
            try {
                Log.v("", "presenceP(" + presence.toString() + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
            String sender = presence.getUuid();
            String presenceString = presence.getEvent().toString();
            otherUsers.add(sender);
        }
    }

    //REVISE THIS

    /**
     * Replaces the image in the ImageView with the image to be guessed after
     * downloading it from GoogleImages.
     *
     * Code is a modified version of code derived from the following:
     *
     * https://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
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


    /**
     * Searches Google Images for the first image related to the passed query.
     * @param image The term being searched for.
     * @return A URL (as a String) of the first image in Google images related to the image passed.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public String getImage(String image) throws IOException, GeneralSecurityException {
        //Instance Customsearch
        Customsearch cs = new Customsearch.Builder(AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                null)
                .setApplicationName("google images for pubnub")
                .setGoogleClientRequestInitializer(new CustomsearchRequestInitializer(Constants.GOOGLE_CSE_KEY))
                .build();

        //Set search parameter
        Customsearch.Cse.List list = cs.cse().list(image).setCx("003540613111458598946:owqp-3vrfz0");

        //Execute search
        Search result = list.setSearchType("image").execute();
        if (result.getItems()!=null) {
            Log.d("ASDFASDF", result.getItems().get(0).getLink());
            return result.getItems().get(0).getLink();
        }
        return "";
    }

    private View.OnClickListener sendListener = new View.OnClickListener(){
        public void onClick(View v) {
            publish(mTextMessage);
        }
    };

    /**
     * Pushes the latest message to PubNub.
     *
     * Based off of (but not directly copied from) the PubNub Android tutorial.
     * https://www.pubnub.com/tutorials/android/chat-basics/
     *
     * @param view The TextView from which the message is being pulled.
     */
    public void publish(View view) {
        final String mMessage = ((TextView) game.this.findViewById(R.id.editText)).getText().toString();
        final Map<String, String> message = new TreeMap<>();
        message.put("sender", Constants.username);
        message.put("message", mMessage);
        message.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
        game.this.pn.publish().channel(Constants.GAME_CHANNEL).message(message).async(
                new PNCallback() {
                    @Override
                    public void onResponse(Object result, PNStatus pnStatus) {
                        try {
                            if (!pnStatus.isError()) {
                                EditText text = game.this.findViewById(R.id.editText);
                                text.setText("");
                                Log.v("", "publish(" + result + ")");
                            } else {
                                Log.v("", "publishErr(" + pnStatus + ")");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }



}
