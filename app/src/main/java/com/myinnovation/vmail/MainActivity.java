package com.myinnovation.vmail;

import static android.app.appsearch.SetSchemaRequest.READ_EXTERNAL_STORAGE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.myinnovation.vmail.Config.Config;
import com.myinnovation.vmail.Service.SendMail;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private TextView status;
    private EditText To,Subject,Message,Attachment;
    private int numberOfClicks;
    private boolean IsInitialVoiceFinshed;
    private ImageView restart;

    Uri fileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        IsInitialVoiceFinshed = false ;
        restart = findViewById(R.id.refreshBtn);
        status = findViewById(R.id.status);
        To =  findViewById(R.id.to);
        Subject  = findViewById(R.id.subject);
        Message = findViewById(R.id.message);
        Attachment = findViewById(R.id.attachment);
        numberOfClicks = 0;

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported");
                }
                speak("Welcome to voice mail. Tell me the mail address to whom you want to send mail?");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        IsInitialVoiceFinshed=true;
                    }
                }, 6000);
            } else {
                Log.e("TTS", "Initialization Failed!");
            }
        });



        restart.setOnClickListener(view -> {
            To.setText("");
            Subject.setText("");
            Message.setText("");
            numberOfClicks = 0;
        });
    }

    private void speak(String text){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void layoutClicked(View view)
    {
        if(IsInitialVoiceFinshed) {
            numberOfClicks++;
            listen();
        }
    }

    private void listen(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }

//    private void sendEmail() {
//        //Getting content for email
//        String email = To.getText().toString().trim();
//        String subject = Subject.getText().toString().trim();
//        String message = Message.getText().toString().trim();
//
//        //Creating SendMail object
//        SendMail sm = new SendMail(this, email, subject, message);
//
//        //Executing sendmail to send email
//        sm.execute();
//    }

    private void exitFromApp()
    {
        this.finishAffinity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100&& IsInitialVoiceFinshed){
            IsInitialVoiceFinshed = false;
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(result.get(0).equals("cancel"))
                {
                    speak("Cancelled!");
                    exitFromApp();
                }
                else {

                    switch (numberOfClicks) {
                        case 1:
                            String to;
                            to= result.get(0).replaceAll("underscore","_");
                            to = to.replaceAll("\\s+","");
                            to = to + "@gmail.com";
                            To.setText(to.toLowerCase());
                            status.setText("Subject?");
                            speak("What should be the subject?");

                            break;
                        case 2:

                            Subject.setText(result.get(0));
                            status.setText("Message?");
                            speak("Give me message");
                            break;
//                        case 3:
//                            Message.setText(result.get(0));
//                            status.setText("ur mail");
//                            speak("Give ur mail address");
//                            break;
//                        case 4 :
//                            Config.EMAIL=result.get(0);
//                            status.setText("password");
//                            speak("provide ur password");
//                            break;
                        case 3 :
                            Message.setText(result.get(0));
                            speak("Tell me file name that you want to send");
                            break;
                        case 4 :
                            String fileName = result.get(0);
                            fileName = fileName.trim();
                            Attachment.setText(fileName);
                            getFileFromFileManager(fileName);
                            if(fileUri == null){
                                speak("No file exist with this name, give another name");
                            }
//                            Config.PASSWORD =result.get(0);
                            status.setText("Confirm?");
//                            speak("Please Confirm the mail\n To : " + To.getText().toString() + "\nSubject : " + Subject.getText().toString() + "\nMessage : " + Message.getText().toString() +"your mail "+Config.EMAIL+"your password" +Config.PASSWORD + "\nSpeak Yes to confirm");
                            speak("Please Confirm the mail\n To : " + To.getText().toString() + "\nSubject : " + Subject.getText().toString() + "\nMessage : " + Message.getText().toString() + "\nSpeak Yes to confirm");
                            break;

                        default:
                            if(result.get(0).equals("yes"))
                            {
                                status.setText("Sending");
                                speak("Sending the mail");
//                                sendEmail();
                                sendEmailUsingGmail();
                            }else
                            {
                                status.setText("Restarting");
                                speak("Please Restart the app to reset");
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        exitFromApp();
                                    }
                                }, 4000);
                            }
                    }

                }
            }
            else {
                switch (numberOfClicks) {
                    case 1:
                        speak(" whom you want to send mail?");
                        break;
                    case 2:
                        speak("What should be the subject?");
                        break;
                    case 3:
                        speak("Give me message");
                        break;
//                    case 4:
//                        speak("provide ur mail");
//                        break;
//                    case 5:
//                        speak("provide ur password");
//                        break;
                    default:
                        speak("say yes or no");
                        break;
                }
                numberOfClicks--;
            }
        }
        IsInitialVoiceFinshed=true;
    }

    private void getFileFromFileManager(String fileName) {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            //ask for permission
            ActivityCompat.requestPermissions(this,new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE},READ_EXTERNAL_STORAGE);
        }
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        fileName = fileName.trim()+".txt";
        File file = new File(path, fileName);
        Log.println(Log.VERBOSE, "FilePath", file.toString());
        if(file.exists()){
            fileUri = Uri.fromFile(file);
            Toast.makeText(this, "I got file", Toast.LENGTH_SHORT).show();
        }
        tts.speak("I didn't got file file", TextToSpeech.QUEUE_FLUSH, null);
    }

    private void sendEmailUsingGmail() {

        String email = To.getText().toString().toLowerCase().trim();
        String subject = Subject.getText().toString().trim();
        String message = Message.getText().toString().trim();

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { email });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

// Attach file
        if(fileUri == null){
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            String fileName = "fee.pdf";
            File file = new File(path, fileName);
            fileUri = Uri.fromFile(file);
        }
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

// Launch Gmail app
        emailIntent.setPackage("com.google.android.gm");
        startActivity(emailIntent);
    }
}