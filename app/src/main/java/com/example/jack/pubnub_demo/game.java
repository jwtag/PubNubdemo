package com.example.jack.pubnub_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.CustomsearchRequestInitializer;
import com.google.api.services.customsearch.model.Search;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class game extends AppCompatActivity {
    private PubNub pn;
    private TextView mTurn;
    private ImageView image;
    private Button yes;
    private Button no;
    private View mSend;
    private boolean isGuesser; //If the this player is playing as a guesser.
    private boolean waitingForImage; //If the current message is to be interpreted as an image to be guessed
    private PubSubPnCallback sub;
    private long timeJoined;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.mTurn = findViewById(R.id.messageView);

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
        isGuesser = true;
        this.timeJoined = System.currentTimeMillis();
        PubSubPnCallback sub = new PubSubPnCallback();
        pn.addListener(sub);
        this.pn.subscribe().channels(Arrays.asList(Constants.GAME_CHANNEL)).execute();
        this.publish("ISANYONEHERE"+ timeJoined);
        waitingForImage = true;


        //Sets up UI elements.
        image = findViewById(R.id.imageView2);
        mSend = findViewById(R.id.button2);
        mSend.setOnClickListener(sendListener);
        yes = findViewById(R.id.yesButton);
        no = findViewById(R.id.noButton);
        yes.setOnClickListener(yesListener);
        no.setOnClickListener(noListener);

        goTime();
    }

    /**
     * Makes the program start the game.
     */
    public void goTime(){
        mTurn = findViewById(R.id.messageView);
        if (isGuesser){
            mTurn.setText("You are the Guesser");
        } else {
            mTurn.setText("You need to submit a word.");
        }
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
                if (messageConverted.equals("Yes! Your turn.")){
                    isGuesser = !isGuesser;
                    waitingForImage = !waitingForImage;
                    goTime();
                    image.setImageResource(R.drawable.hrglass);
                    image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    TextView messageView = findViewById(R.id.oppMsg);
                    messageView.setText(messageConverted);
                    mTurn = findViewById(R.id.messageView);
                } else if (messageConverted.contains("ISANYONEHERE")){
                    Log.v("TJ", "" + timeJoined);

                    long otherTimeJoined  = Long.parseLong(messageConverted.substring(12));
                    Log.v("OTJ", "" + otherTimeJoined);

                    isGuesser = otherTimeJoined == timeJoined;
                    goTime();
                } else {
                    if (messageConverted.contains("I_M_A_G_E") && waitingForImage) { //For when the opponent initially sends the image to be guessed.
                        //Then we search google for first img's url, store it
                        String imageFromMessage = getImage(messageConverted.substring(9));
                        Log.v("URL", imageFromMessage);
                        //Then download image and replace image with it's contents.
                        new DownloadImageTask(image).execute(imageFromMessage);
                        waitingForImage = false;
                    } else { //For receiving plain text-based messages from opponent.
                        TextView messageView = findViewById(R.id.oppMsg);
                        messageView.setText(messageConverted);
                    }
                }
                goTime();
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
        }
    }

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
            bmImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
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
        Customsearch.Cse.List list = cs.cse().list(image).setCx("016348624239509613727:qfywoa3331u");

        //Execute search
        Search result = list.setSearchType("image").execute();
        if (result.getItems()!=null) {
            Log.d("LINK", result.getItems().get(0).getLink());
            return result.getItems().get(0).getLink();
        }
        return "";
    }

    /**
     * Listener for if the "SEND" button is clicked.
     */
    private View.OnClickListener sendListener = new View.OnClickListener(){
        public void onClick(View v) {
            publish(((TextView) game.this.findViewById(R.id.editText)).getText().toString());
        }
    };

    /**
     * Listener for if the "YES" button is clicked.
     */
    private View.OnClickListener yesListener = new View.OnClickListener(){
        public void onClick(View v) {
            publish("Yes! Your turn.");
            game.this.findViewById(R.id.editText).setVisibility(View.VISIBLE);
            yes.setVisibility(View.GONE);
            no.setVisibility(View.GONE);
            goTime();
        }
    };

    /**
     * Listener for if the "NO" button is clicked.
     */
    private View.OnClickListener noListener = new View.OnClickListener(){
        public void onClick(View v) {
            publish("No.  Guess again.");
        }
    };

    /**
     * Pushes the latest message to PubNub.
     *
     * Based off of (but not directly copied from) the PubNub Android tutorial.
     * https://www.pubnub.com/tutorials/android/chat-basics/
     *
     * @param mMessage The message being published
     */
    public void publish(String mMessage) {
        final Map<String, String> message = new TreeMap<>();
        if (!isGuesser && waitingForImage){
            mMessage = "I_M_A_G_E" + mMessage;
            this.findViewById(R.id.editText).setVisibility(View.GONE);
            yes.setVisibility(View.VISIBLE);
            no.setVisibility(View.VISIBLE);
        }
        if (!isGuesser || !waitingForImage) {
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



}
