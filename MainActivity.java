/*
 * Title: Audio Process
 * Author: Roberto Becerra
 * Date: 26 March 2015
 * Contact: io_robertob@hotmail.com
 *
 * Final project of Applied DSP by UCSD, Android app to process audio from a WAV file stored in the
 * resources folder of project, filter it and play it back.
 *
 * */
package com.example.rbecerra.myapplication;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.media.*;
import java.io.*;
import java.lang.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends ActionBarActivity {

    /* Instances of Audio Track and playAudio Thread */
    public AudioTrack audioOut = null;
    private volatile playAudio audioOutThread;

    public static int seekBarLastProgress1 = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /* Volume Control available for this App */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

    }

    @Override
    protected void onResume() {
        super.onResume();

        /* Creation and start of Audio Playback Thread */
        audioOutThread = new playAudio();
        audioOutThread.start();

        final SeekBar seekbar1 = (SeekBar) findViewById(R.id.seekBar);

        seekbar1.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser){

						/* Send values only they are new*/
                if(progress != seekBarLastProgress1){
                    audioOutThread.changeOrder(progress);

                    TextView taps = (TextView) findViewById(R.id.textView2);


                    taps.setText(Integer.toString(progress+2));



                }

            }
            public void onStartTrackingTouch(SeekBar seekBar){
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                // TODO Auto-generated method stub
            }
        });

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

    @Override
    public void onDestroy(){
        super.onDestroy();

        audioOutThread.closeThread();

    }


    /* Start Playback when button is pressed. */
    public void playRhapsody(View view){

        audioOutThread.playThread();

    }

    /* Stop Playback when button is pressed */
    public void stopPlaying(View view){

        audioOutThread.stopMusic();

    }

    /* Toggle filter when toggle button is switched.*/
    public void filter(View view){

        audioOutThread.filterOnOff(((ToggleButton) findViewById(R.id.toggleButton)).isChecked());

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// A U D I O     T H R E A D ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /* This is the playAudio Thread which sets up an AudioTrack object to play
    *data from a WAV file. It is done by reading the file, dumping the data into
    * a bytes array, convert it into shorts, since the WAV file is formatted in 16 bits, meaning
    * we have to take two bytes, little endian as one sample value.
    * It after process it with a Comb filter of order 'order'. This process is explained in detail
    * further.*/
    public class playAudio extends Thread implements Runnable {

        boolean filtered = false;

        int sampleRate = 44100;
        int i = 0;
        int j = 0;
        int k = 0;
        int order = 1;
        int playStop = 0;
        int SR;
        int first = 1;
        int flag = 0;

        byte [] music;
        short[] musicFiltered;
        short[] music2Short;
        short[] buf;

        InputStream is;

        private int minSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);


        @Override
        public void run() {
            super.run();

            Thread thisThread = Thread.currentThread();

            /* Thread Loop, until audioOutThread is made null. */
            while (audioOutThread == thisThread) {

                if (flag == 1){

                    try {

                        audioOut.play();

                        while (((i = is.read(music)) != -1) && (playStop == 1) ) {

                            /* Take the bytes vector in little endian and make it a shorts vector */
                            ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music2Short);

                            //////////////////////////////////////////
                            /*    START  OF  FILTER  ALGORITHM      */
                            //////////////////////////////////////////
                            /* Comb filter of "order" order.
                             * Data from the WAV file is read into the 'music'
                             * bytes vector, which is transformed int o a shorts
                             * vector. Later the output shorts vector 'musicFiltered'
                             * is filled with the following algorithm
                             *
                             * musicFiltered[n] = musicFiltered[n] + S musicFiltered[n-i]
                             *
                             * where the sum index 'i' goes from 1 to (order-1)
                             *
                             * For n = 0, and before n = (order-1) we use a shorts
                             * vector 'buf' that stores the (order-1) last samples
                             * from the previous frame that was read into 'music'
                             *
                             * Then, finally, 'buf' is filled with this last samples. And repeat
                             * the same process until the WAV file is finished. */
                            //////////////////////////////////////////


                            /* First order-1 samples of output vector, processed with data previously
                             * stored in the 'buf' buffer */
                            for(j = 0; j < (order-1); ++j){

                                musicFiltered[j] = (short)((music2Short[0] + buf[0]) / order);

                                for(k = 1; k <= j; ++k){
                                    musicFiltered[j] += (short)(music2Short[k]/order);
                                }

                                for(k = 1; k < order-1-j; ++k){
                                    //Log.d("AudioIO", "order = " + order + " j = " + j + " k = " + k);
                                    musicFiltered[j] += (short)(buf[k]/order);
                                }

                            }

                            /* Sum of comb filter for the rest of the data frame */
                            for (j = order-1; j < music2Short.length; ++j) {

                                musicFiltered[j] = (short) (music2Short[j]/order);

                                for(k = 1; k < order; ++k){
                                    musicFiltered[j] += (short) (music2Short[j-k]  / order);
                                }
                            }

                            /* Beginning of WAV File, skip first 44 header bytes (22 shorts)*/
                            if(first == 1){

                                if(filtered){
                                    /* Hardcoded since this is the WAV format number of shorts of header */
                                    audioOut.write(musicFiltered, 22, i/2);
                                }
                                else {
                                    /* Hardcoded since this is the WAV format number of bytes of header */
                                    audioOut.write(music,44, i);
                                }

                            }else{/* Not the beginning of file, do not skip bytes */
                                if(filtered){
                                    /* Hardcoded, write and do not skip bytes in read data*/
                                    audioOut.write(musicFiltered, 0, i/2);
                                }
                                else {
                                    /* Hardcoded, write and do not skip bytes in read data*/
                                    audioOut.write(music,0, i);
                                }

                            }

                            /* Fill Buffer for initial samples and clean the previous data */
                            for(j = 0; j < order-1; ++j){

                                buf[j] = music2Short[music2Short.length - (j+1)];
                                musicFiltered[j] = 0;

                            }

                            /* Reset the 'beginning of WAF file' condition */
                            first = 0;

                            /////////////////////////////////////
                            /*    END OF FILTER ALGORITHM      */
                            /////////////////////////////////////


                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.d("IO_Sound", "audioOut.Play: finished playing.");
                    audioOut.stop();
                    audioOut.release();
                    flag = 0;
                    first = 1;

                    this.initialize();



                }

            }

        }

        /* Public constructor of the thread class for the Server  */
        public playAudio() {

            this.initialize();

            SR = audioOut.getSampleRate();

            if ( (minSize/2) % 2 != 0 ) {
                /*If minSize divided by 2 is odd, then subtract 1 and make it even*/
                musicFiltered   = new short [((minSize/2) - 1)/2];
                music2Short     = new short [((minSize/2) - 1)/2];
                music           = new byte  [(minSize/2) - 1];
            }
            else {
                /* Else it is even already */
                musicFiltered   = new short [minSize/4];
                music2Short     = new short [minSize/4];
                music           = new byte  [minSize/2];
            }

            /*This buffer will keep the samples needed for the filter summation*/
            buf = new short[order-1];
        }

        /* Thread method to start playback */
        public void playThread() {

            playStop    = 0;        // Reset Play / Stop instruction
            flag        = 1;        // Used for when reading the input file has finished.
            playStop    = 1;        // Set ready to play.
            first       = 1;        // Beginning of WAV file, flag to skip 44 bytes.
        }

        /* Thread method to stop playback */
        public void stopMusic(){
            playStop    = 0;
        }


        /* This functions closes the thread by making serverThread1 null */
        public void closeThread() {
            audioOutThread = null;
        }

        public void initialize(){

            /* Different files to choose from */
            is = getResources().openRawResource(R.raw.rhapsody);
            //is = getResources().openRawResource(R.raw.symphonix);
            //is = getResources().openRawResource(R.raw.noise_44100_16bit);

            audioOut = new AudioTrack(
                    AudioManager.STREAM_MUSIC,          // Stream Type
                    sampleRate,                         // Initial Sample Rate in Hz
                    AudioFormat.CHANNEL_OUT_MONO,       // Channel Configuration
                    AudioFormat.ENCODING_PCM_16BIT,     // Audio Format
                    minSize,                            // Buffer Size in Bytes
                    AudioTrack.MODE_STREAM);            // Streaming static Buffer

        }


        public void filterOnOff(boolean isFiltered){
            /* Filter on and off */
            filtered = isFiltered;
        }


        public void changeOrder(int newOrder){

            order = newOrder + 1;
            buf = new short[order-1];
        }


    }


}
