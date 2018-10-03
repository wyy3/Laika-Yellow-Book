package com.laika.laika_yellow_book;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.QUEUE_ADD;


public class NewEntryActivity extends AppCompatActivity implements AsyncResponse,TwinCalfDialogListener {
    private DbHelper myDb;
    private TextToSpeech mTTS;
    private String[] labels;
    private int index = 0;
    private EditText currEditText;
    private EditText[] editTexts;
    private DataLine data;
    private boolean isIndividual = false;
    private TextInputLayout[] textInputLayout;
    private InputValidation inputValidation;
    private int apiIndex;
    private ArrayList<DataLine> twins;
    private int twinCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_entry);
        myDb = new DbHelper(this);
        data = new DataLine();
        twins = new ArrayList<DataLine>();

        inputValidation = new InputValidation();
        inputValidation.setData(data);
        editTexts = new EditText[11];
        textInputLayout = new TextInputLayout[11];

        TextView addTwinCalf = findViewById(R.id.tv_addTwinCalf);
        addTwinCalf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
            if(twinCount < 3)
                openTwinCalfDialog();
            else
                Toast.makeText(NewEntryActivity.this, "Max of 3 twin calves allowed", Toast.LENGTH_LONG).show();
                }
        });
        //create toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("New Entry");
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //get all label values
        LinearLayout layout = findViewById(R.id.linearLayout1);
        labels = new String[11];
        int c = 0;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if(v instanceof TextInputLayout) {
                textInputLayout[c] = (TextInputLayout)v;
                editTexts[c] = ((TextInputLayout) v).getEditText();
                labels[c] = ((TextInputLayout) v).getHint().toString();
                c++;
            }
        }

        setDateTimePicker(editTexts[1],1);
        setDateTimePicker(editTexts[4],4);

        int tag = 0;
        for (final EditText et : editTexts) {
            et.setTag(tag);
            tag++;
            et.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        if (motionEvent.getRawX() >= (et.getRight() - et.getCompoundDrawables()[2].getBounds().width())) {
                            isIndividual = true;
                            currEditText = (EditText) view;
                            currEditText.requestFocus();
                            if (currEditText.getTag() != null) {
                                index = (int) currEditText.getTag();
                            }
                            askSpeechInput(currEditText);
                            return true;
                        }
                    }
                    return false;
                }
            });

            et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    //removes error message when field is blank
                    if(s.length() == 0) {
                        if(et.getTag()!=null) {
                            int i = (int) et.getTag();
                            textInputLayout[i].setError(null);
                        }
                    }
                }
            });

            //validate input
            et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        String input = et.getText().toString().trim();
                        if (!input.isEmpty()) {
                            int i = -1;
                            if (view.getTag() != null)
                                i = (int) et.getTag();
                            String err = inputValidation.validate(input,i);
                            if(!err.isEmpty()){
                                textInputLayout[i].setError(err);
                            }
                            else
                                textInputLayout[i].setError(null);
                        }
                    }
                    else {
                        EditText currFocus = (EditText) getCurrentFocus();
                        if(currFocus != null & currFocus != view)
                            currFocus.clearFocus();
                        currEditText = (EditText) view;
                    }
                }
            });
        }

        initTTS();
    }

    private void setDateTimePicker(final EditText ed, final int index) {
        final Calendar myCalendar = Calendar.getInstance();
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                ed.setText(format.format(myCalendar.getTime()));
                if(index == 1) {
                    data.dueCalveDate = myCalendar.getTime();
                    textInputLayout[index].setError(null);
                }
                else if(index == 4) {
                    data.calvingDate = myCalendar.getTime();
                    textInputLayout[index].setError(null);
                }
            }
        };

        ed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(NewEntryActivity.this,date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    private void initTTS() {
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String s) {}

                        @Override
                        public void onDone(String s) {

                            if (s.equals("input")) {
                                if (!isIndividual && currEditText != null) {
                                    //read next label
                                    HashMap<String, String> map = new HashMap<String, String>();
                                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "label");
                                    mTTS.speak(labels[index], QUEUE_ADD, map);
                                    //pause for 1 sec before speech starts
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
                                        android.util.Log.d("new entry", ex.toString());
                                    }
                                    askSpeechInput(currEditText);
                                }
                            }
                            else if(s.equals("finish")){
                                finish();
                            }
                        }

                        @Override
                        public void onError(String s) {}
                    });
                    int result = mTTS.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
    }

    private final int REQ_CODE_SPEECH_INPUT = 100;

    public void askSpeechInput(View view) {
        if (view == findViewById(R.id.btn_VoiceInput)) {
            index = 0;
            isIndividual = false;
            currEditText = editTexts[0];
            currEditText.requestFocus();

            HashMap<String, String> map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "label");
            mTTS.speak(labels[index], QUEUE_ADD, map);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Log.e("new entry", ex.toString());
            }
        }
        initSTT();
    }

    private void initSTT() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, labels[index]);
        if(!isIndividual)
            index++;
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Log.e("STT", "Initialization failed " + a.getMessage());
        }
    }

    //Call after valid result returns
    private void speakResult(View view) {
        currEditText = (EditText) view;
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "input");
        mTTS.speak(currEditText.getText().toString(), QUEUE_ADD, map);
        if (!isIndividual) {
            //iterate through all textboxes
            if(findViewById(currEditText.getNextFocusDownId()) != null){
                currEditText = findViewById(currEditText.getNextFocusDownId());
                currEditText.requestFocus();
            }
        }
    }

    private boolean hasInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            return true;
        } else {
            return false;
        }
    }

    private boolean isKeyword (View view, String text) {
        currEditText = (EditText) view;
        String keyword = text.trim();
        String[] keywords = keyword.split(" ");
        if(keywords.length == 1) {
            switch (keyword) {
                case "clear":
                    currEditText.setText("");
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "label");
                    mTTS.speak(labels[index], QUEUE_ADD, map);
                    askSpeechInput(currEditText);
                    return true;
                case "skip":
                    if(findViewById(currEditText.getNextFocusDownId()) != null){
                        currEditText = findViewById(currEditText.getNextFocusDownId());
                        currEditText.requestFocus();
                        if (!isIndividual && currEditText != null) {
                            //read next label
                            map = new HashMap<String, String>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "label");
                            mTTS.speak(labels[index], QUEUE_ADD, map);
                            //pause for 1 sec before speech starts
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                android.util.Log.d("new entry", ex.toString());
                            }
                            askSpeechInput(currEditText);
                        }
                    }
                    return true;
                case "previous":
                    if(findViewById(currEditText.getNextFocusUpId()) != null){
                        currEditText = findViewById(currEditText.getNextFocusUpId());
                        currEditText.requestFocus();
                        if (!isIndividual && currEditText != null) {
                            if(currEditText.getTag() != null)
                                index = (int) currEditText.getTag();
                            //read previous label
                            map = new HashMap<String, String>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "label");
                            mTTS.speak(labels[index], QUEUE_ADD, map);
                            //pause for 1 sec before speech starts
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                android.util.Log.d("new entry", ex.toString());
                            }
                            askSpeechInput(currEditText);
                        }
                    }
                    return true;
                case "safe":
                case "save":
                    boolean isSuccess = AddData(currEditText);
                    map = new HashMap<String, String>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "finish");
                    if(isSuccess) {
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        mTTS.speak("New entry saved", QUEUE_ADD, map);

                    }
                    else{
                        map = new HashMap<String, String>();
                        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "error");
                        mTTS.speak("Please ensure that all fields are valid", QUEUE_ADD, map);
                    }
                    return true;
                case "discard":
                    //alert
                    map = new HashMap<String, String>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "message");
                    mTTS.speak("Current entry is discarded", QUEUE_ADD, map);
                    data = new DataLine();
                    clearEditText();
                    return true;
            }
        }
        return false;
    }

    private void clearEditText() {
        for(EditText et : editTexts) {
            et.setText("");
        }
        editTexts[0].requestFocus();
    }
    boolean isKeyword = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != intent) {
                    ArrayList<String> result = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //check keywords
                    isKeyword = isKeyword(currEditText, result.get(0));
                    if(isKeyword)
                        return;

                    //check internet connection
                    try {
                        if (currEditText == editTexts[1]) {
                            boolean hasInternet = hasInternet();
                            if (hasInternet) {
                                currEditText.setText(result.get(0));
                                currEditText.setEnabled(false);
                                ValidateResultsAPI validateResult = new ValidateResultsAPI();
                                validateResult.delegate = this;
                                apiIndex = 1;
                                validateResult.execute(result.get(0));
                            } else {
                                Date date = inputValidation.parseDate(result.get(0));
                                if (date == null) {
                                    textInputLayout[1].setError("Format: first of August 2018");
                                    currEditText.setText(result.get(0));
                                } else {
                                    data.dueCalveDate = date;
                                    currEditText.setText(new SimpleDateFormat("yyyy-MM-dd").format(date));
                                }
                                speakResult(currEditText);
                            }
                        } else if (currEditText == editTexts[4]) {
                            boolean hasInternet = hasInternet();
                            if (hasInternet) {
                                currEditText.setText(result.get(0));
                                currEditText.setEnabled(false);
                                ValidateResultsAPI validateResult = new ValidateResultsAPI();
                                validateResult.delegate = this;
                                apiIndex = 4;
                                validateResult.execute(result.get(0));
                            } else {
                                Date date = inputValidation.parseDate(result.get(0));
                                if (date == null) {
                                    textInputLayout[4].setError("Format: first of August 2018");
                                    currEditText.setText(result.get(0));
                                } else {
                                    data.calvingDate = date;
                                    currEditText.setText(new SimpleDateFormat("yyyy-MM-dd").format(date));
                                }
                                speakResult(currEditText);
                            }
                        } else {
                            currEditText.setText(result.get(0));
                            speakResult(currEditText);
                        }
                    } catch (Exception e) {
                        Log.e("STT", e.getMessage());
                    }
                }
            }
            break;
        }
    }

    public boolean AddData(View view) {
        if(editTexts[0].getText().toString().isEmpty()){
            textInputLayout[0].setError("Cow number cannot be blank!");
            editTexts[0].requestFocus();
            Toast.makeText(NewEntryActivity.this, "Error, Cow Number cannot be blank!", Toast.LENGTH_LONG).show();
            return false;
        }
        else if(editTexts[4].getText().toString().isEmpty()) {
            textInputLayout[4].setError("Calving date cannot be blank!");
            editTexts[4].requestFocus();
            Toast.makeText(NewEntryActivity.this, "Error, Calving Date cannot be blank!", Toast.LENGTH_LONG).show();
            return false;
        }

        String input = currEditText.getText().toString();
        if (!input.isEmpty()) {
            currEditText.getOnFocusChangeListener().onFocusChange(currEditText,false);
        }

        //check if has invalid input
        for (TextInputLayout layout: textInputLayout) {
            if(!TextUtils.isEmpty(layout.getError())) {
                layout.getEditText().requestFocus();
                Toast.makeText(NewEntryActivity.this, "Please make sure that all fields are valid!", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        boolean isSuccessful = myDb.insertData(data.cowNum, data.dueCalveDate, data.sireOfCalf, data.calfBW, data.calvingDate, data.calvingDiff, data.condition, data.sex, data.fate, data.calfIndentNo, data.remarks);
        if (isSuccessful) {
            if(!isKeyword)
                openDialog();
            clearEditText();
            return true;
        }
        else {
            Toast.makeText(NewEntryActivity.this, "Insertion failed", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void openDialog() {
        PopupDialog dialog = new PopupDialog();
        dialog.setContext(this);
        dialog.show(getSupportFragmentManager(), "popup dialog");
    }

    @Override
    public void processFinish(final String output) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            if(apiIndex==1) {
                data.dueCalveDate = format.parse(output);
                editTexts[apiIndex].setText(format.format(data.dueCalveDate));
                textInputLayout[apiIndex].setError(null);
                editTexts[apiIndex].setEnabled(true);
                speakResult(editTexts[apiIndex]);
            } if(apiIndex==4){
                data.calvingDate = format.parse(output);
                editTexts[apiIndex].setText(format.format(data.calvingDate));
                textInputLayout[apiIndex].setError(null);
                editTexts[apiIndex].setEnabled(true);
                speakResult(editTexts[apiIndex]);
            }
        } catch (ParseException e) {
           textInputLayout[apiIndex].setError("Invalid date, please try again.");
           editTexts[apiIndex].setEnabled(true);
           speakResult(editTexts[apiIndex]);
        }
    }

    private void openTwinCalfDialog(){
        TwinCalfDialog twinCalfDialog = new TwinCalfDialog();
        twinCalfDialog.show(getSupportFragmentManager(), "twin dialog");
    }

    @Override
    public void passData(int calfID, String calfSex, Double calfBW, String calfCondition) {
        String id = String.valueOf(calfID);
        switch (twinCount) {
            case 0:
                TextView twin1 = findViewById(R.id.twin1);
                twin1.setVisibility(View.VISIBLE);
                twin1.setText(id);
                break;
            case 1:
                TextView twin2 = findViewById(R.id.twin2);
                twin2.setVisibility(View.VISIBLE);
                twin2.setText(id);
                break;
            case 2:
                TextView twin3 = findViewById(R.id.twin3);
                twin3.setVisibility(View.VISIBLE);
                twin3.setText(id);
                break;
        }
        twinCount++;
        editTexts[8].requestFocus();
        DataLine twin = new DataLine(data.cowNum, data.dueCalveDate,data.sireOfCalf,calfBW,data.calvingDate,data.calvingDiff,calfCondition,calfSex,data.fate,calfID,data.remarks);
        twins.add(twin);
    }
}