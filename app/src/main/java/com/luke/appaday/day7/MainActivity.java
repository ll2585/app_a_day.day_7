package com.luke.appaday.day7;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends ActionBarActivity implements TextToSpeech.OnInitListener {
    private Locale currentSpokenLang = Locale.US;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this, this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getInfo(View view) {

        EditText role_edit_text = (EditText) findViewById(R.id.type_role_edit_text);

        // If the user entered words to translate then get the JSON data
        if(!isEmpty(role_edit_text)){

            Toast.makeText(this, "Getting Role Info",
                    Toast.LENGTH_LONG).show();

            // Calls for the method doInBackground to execute
            new GetXMLData().execute();

        } else {

            // Post an error message if they didn't enter words
            Toast.makeText(this, "Enter Role",
                    Toast.LENGTH_SHORT).show();

        }

    }

    public void readRoleInfo(View view) {

        // Set the voice to use
        textToSpeech.setLanguage(currentSpokenLang);

        String roleInfo = ((TextView) findViewById(R.id.role_info)).getText().toString();
        // QUEUE_FLUSH deletes previous text to read and replaces it
        // with new text
        textToSpeech.speak(roleInfo, TextToSpeech.QUEUE_FLUSH, null);
        System.out.println("TRYING TO READ");

    }

    protected boolean isEmpty(EditText editText){

        // Get the text in the EditText convert it into a string, delete whitespace
        // and check length
        return editText.getText().toString().trim().length() == 0;

    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(currentSpokenLang);

            // If language data or a specific language isn't available error
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language Not Supported", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Text To Speech Failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void speakTheRole(View view) {

        // Starts an Activity that will convert speech to text
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Use a language model based on free-form speech recognition
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Recognize speech based on the default speech of device
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        // Prompt the user to speak
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.spoken_input));

        try{

            startActivityForResult(intent, 100);

        } catch (ActivityNotFoundException e){

            Toast.makeText(this, getString(R.string.stt_not_supported_message), Toast.LENGTH_LONG).show();

        }

    }

    // The results of the speech recognizer are sent here
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        // 100 is the request code sent by startActivityForResult
        if((requestCode == 100) && (data != null) && (resultCode == RESULT_OK)){

            // Store the data sent back in an ArrayList
            ArrayList<String> spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            EditText wordsEntered = (EditText) findViewById(R.id.type_role_edit_text);

            // Put the spoken text in the EditText
            wordsEntered.setText(spokenText.get(0));

        }

    }

    class GetXMLData extends AsyncTask<Void, Void, Void> {

        String stringToPrint = "";

        @Override
        protected Void doInBackground(Void... voids) {

            String xmlString = "";

            String role = ((EditText) findViewById(R.id.type_role_edit_text)).getText().toString();

            role = role.trim().replace(" ", "_");

            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());

            HttpGet httpGet = new HttpGet("https://arcane-depths-3989.herokuapp.com/get_roles?role=" + role);

            httpGet.setHeader("Content-type", "text/xml");

            InputStream inputStream = null;

            try{

                HttpResponse response = httpClient.execute(httpGet);

                HttpEntity entity = response.getEntity();

                inputStream = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                StringBuilder sb = new StringBuilder();

                String line = null;

                while((line = reader.readLine()) != null){

                    sb.append(line);


                }

                xmlString = sb.toString();

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

                factory.setNamespaceAware(true);

                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(new StringReader(xmlString));

                int eventType = xpp.getEventType();

                while(eventType != XmlPullParser.END_DOCUMENT){

                    if(eventType == XmlPullParser.TEXT){

                        stringToPrint = stringToPrint + xpp.getText() + "\n";

                    }

                    eventType = xpp.next();

                }

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (UnsupportedEncodingException e){
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            TextView role_info = (TextView) findViewById(R.id.role_info);

            // Make the TextView scrollable
            role_info.setMovementMethod(new ScrollingMovementMethod());

            String role_description = stringToPrint;

            role_info.setText(role_description);

        }

    }
}
