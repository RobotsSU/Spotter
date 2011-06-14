/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.eyesfree.voicecontrol;

import android.content.Context;
import android.content.res.Resources;
import android.speech.srec.MicrophoneInputStream;
import android.speech.srec.Recognizer;
import android.util.Config;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Voice command recognition class. Takes a list of input commands and returns
 * recognized commands.
 *
 * @author alanv@google.com (Alan Viverette)
 */
public class GrammarRecognizer {
    private static final String TAG = "GrammarRecognizer";

    private static final String EXTENSION_G2G = "g2g";

    private static final String SREC_DIR = Recognizer.getConfigDir(null);

    private static final int SAMPLE_RATE = 11025;

    private static final int RESULT_LIMIT = 5;

    // Initialization constants
    public static final int SUCCESS = 0;

    public static final int ERROR = -1;

    public static final String KEY_CONFIDENCE = Recognizer.KEY_CONFIDENCE;

    public static final String KEY_LITERAL = Recognizer.KEY_LITERAL;

    public static final String KEY_MEANING = Recognizer.KEY_MEANING;

    private WeakReference<Context> mContext;

    private File mGrammar;

    private Recognizer mSrec;

    private Recognizer.Grammar mSrecGrammar;

    private GrammarListener mListener;

    private Thread mThread;

    private boolean mAlive;

    /**
     * Creates a new GrammarRecognizer for the given Context.
     *
     * @param context The context that will own this object.
     */
    public GrammarRecognizer(Context context) {
        mContext = new WeakReference<Context>(context);

        mAlive = false;
    }

    /**
     * Interrupts the recognition thread. You may call still recognize() after
     * calling this method.
     *
     * @return <code>true</code> on success
     */
    public boolean stop() {
        mAlive = false;

        if (mThread != null) {
            mThread.interrupt();

            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mThread = null;

            return true;
        }

        return false;
    }

    public boolean isListening() {
        return mAlive;
    }

    /**
     * Stops the recognition thread and destroys the recognizer. You may not
     * call any methods on the object after calling this method.
     */
    public void shutdown() {
        stop();

        mSrecGrammar.destroy();
        mSrec.destroy();
    }

    /**
     * Loads a compiled grammar from a resource ID.
     *
     * @param resId The resource ID of the G2G-formatted grammar.
     * @return <code>true</code> on success
     */
    public boolean loadGrammar(int resId) {
        File grammar = unpackGrammar(resId);

        return loadGrammar(grammar);
    }

    /**
     * Loads a compiled grammar from a file.
     *
     * @param grammar A file representing the compiled G2G-formatted grammar.
     * @return <code>true</code> on success
     */
    public boolean loadGrammar(File grammar) {
        boolean successful = false;

        // Check directory contents...
        String[] contents = grammar.getParentFile().list();
        for (String content : contents) {
            Log.e(TAG, content);
        }

        mGrammar = grammar;

        String path = mGrammar.getPath();

        try {
            mSrec = new Recognizer(SREC_DIR + "/baseline11k.par");
            mSrecGrammar = mSrec.new Grammar(path);
            mSrecGrammar.setupRecognizer();
            mSrecGrammar.resetAllSlots();

            successful = true;
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString());

            String[] contents2 = grammar.getParentFile().list();
            for (String content : contents2) {
                Log.e(TAG, "+" + content);
            }

            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (!successful) {
            if (mSrecGrammar != null) {
                mSrecGrammar.destroy();
            }

            if (mSrec != null) {
                mSrec.destroy();
            }

            mSrecGrammar = null;
            mGrammar = null;
            mSrec = null;
        }

        return successful;
    }

    /**
     * Loads the available slots in a grammar using the entries in the supplied
     * GrammarMap. You must load a grammar before calling this method.
     *
     * @param map A populated GrammarMap object.
     * @return <code>true</code> on success
     */
    public boolean loadSlots(GrammarMap map) {
        Context context = mContext.get();

        Resources res = context.getResources();
        File cached = context.getFileStreamPath("compiled." + EXTENSION_G2G);
        cached.deleteOnExit();

        for (Entry<String, List<GrammarEntry>> pair : map.getEntries()) {
            String slot = pair.getKey();
            List<GrammarEntry> entries = pair.getValue();

            for (GrammarEntry entry : entries) {
                mSrecGrammar.addWordToSlot(slot, entry.word, entry.pron, entry.weight, entry.tag);
            }
        }

        // Attempt to compile and save grammar to cache
        try {
            mSrecGrammar.compile();
            cached.getParentFile().mkdirs();
            mSrecGrammar.save(cached.getPath());

            Log.i(TAG, "Compiled grammar to " + cached.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        // Destroy the old grammar
        mSrecGrammar.destroy();
        mSrecGrammar = null;

        // Attempt to load new grammar from cached copy
        try {
            mGrammar = cached;
            mSrecGrammar = mSrec.new Grammar(cached.getPath());
            mSrecGrammar.setupRecognizer();
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    /**
     * Sets this grammar's listener, which will receive recognition results and
     * status updates.
     *
     * @param listener The listener to set.
     */
    public void setListener(GrammarListener listener) {
        mListener = listener;
    }

    public void recognize() {
        stop();

        mThread = new Thread() {
            @Override
            public void run() {
                recognizeThread();
            }
        };
        mThread.start();
    }

    /**
     * Attempts to unpack a grammar resource into cache. The unpacked copy will
     * be deleted when the application exits normally.
     *
     * @param resId The resource ID of the raw grammar resource.
     * @return the file representing by the unpacked grammar
     */
    private File unpackGrammar(int resId) {
        Context context = mContext.get();

        String extension = EXTENSION_G2G;

        Resources res = context.getResources();
        String name = res.getResourceEntryName(resId);
        File grammar = context.getFileStreamPath(name + "." + extension);
        grammar.deleteOnExit();

        try {
            grammar.getParentFile().mkdirs();

            InputStream in = res.openRawResource(resId);
            FileOutputStream out = new FileOutputStream(grammar);

            byte[] buffer = new byte[1024];
            int count = 0;

            while ((count = in.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }

            Log.i(TAG, "Unpacked base grammar to " + grammar.getPath());

            return grammar;
        } catch (IOException e) {
            e.printStackTrace();

            Log.e(TAG, e.toString());

            return null;
        }
    }

    /**
     * Runs the recognition loop.
     */
    private void recognizeThread() {
        if (mAlive) {
            return;
        }

        mAlive = true;

        if (Config.DEBUG) {
            Log.e(TAG, "Started recognition thread");
        }

        InputStream mic = null;
        boolean recognizerStarted = false;

        // Possible results...
        String error = null;
        TreeSet<GrammarResult> results = null;
        boolean failure = false;

        try {
            mic = new MicrophoneInputStream(SAMPLE_RATE, SAMPLE_RATE * 15);

            mSrec.start();
            recognizerStarted = true;

            while (mAlive) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                int event = mSrec.advance();
                if (Config.DEBUG && event != Recognizer.EVENT_INCOMPLETE
                        && event != Recognizer.EVENT_NEED_MORE_AUDIO) {
                    Log.e(TAG, "event=" + event);
                }

                switch (event) {
                    case Recognizer.EVENT_INCOMPLETE:
                    case Recognizer.EVENT_STARTED:
                    case Recognizer.EVENT_START_OF_VOICING:
                    case Recognizer.EVENT_END_OF_VOICING:
                        break;
                    case Recognizer.EVENT_START_OF_UTTERANCE_TIMEOUT:
                        if (Config.DEBUG) {
                            Log.e(TAG, "Didn't hear anything, restarting");
                        }
                        mSrec.stop();
                        mSrec.start();
                        break;
                    case Recognizer.EVENT_NO_MATCH:
                        if (Config.DEBUG) {
                            Log.e(TAG, "No match, return not recognized");
                        }
                        mAlive = false;
                        failure = true;
                        break;
                    case Recognizer.EVENT_RECOGNITION_RESULT:
                        if (Config.DEBUG) {
                            Log.e(TAG, "Recognized something, return it");
                        }
                        mAlive = false;
                        results = getRecognitionResults();
                        break;
                    case Recognizer.EVENT_NEED_MORE_AUDIO:
                        mSrec.putAudio(mic);
                        break;
                    default:
                        if (Config.DEBUG) {
                            Log.e(TAG, "Received invalid event=" + event);
                        }
                        mAlive = false;
                        error = Recognizer.eventToString(event);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

            mListener.onRecognitionError(e.toString());
        } catch (InterruptedException e) {
            // do nothing
        } finally {
            if (mSrec != null && recognizerStarted) {
                mSrec.stop();
            }

            try {
                if (mic != null) {
                    mic.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                mic = null;
            }

            // Wait until the very end to send results, otherwise we might
            // overlap and crash the AudioFlinger permanently!
            if (mListener != null) {
                if (error != null) {
                    mListener.onRecognitionError(error);
                } else if (results != null) {
                    mListener.onRecognitionSuccess(results);
                } else if (failure) {
                    mListener.onRecognitionFailure();
                }
            }
        }
    }

    /**
     * Creates a list of GrammarResult objects and returns them.
     */
    private TreeSet<GrammarResult> getRecognitionResults() {
        TreeSet<GrammarResult> results = new TreeSet<GrammarResult>();

        for (int index = 0; index < mSrec.getResultCount() && index < RESULT_LIMIT; index++) {
            int confidence = Integer.parseInt(mSrec.getResult(index, Recognizer.KEY_CONFIDENCE));
            String literal = mSrec.getResult(index, Recognizer.KEY_LITERAL);
            String meaning = mSrec.getResult(index, Recognizer.KEY_MEANING);

            GrammarResult result = new GrammarResult(meaning, literal, confidence);

            results.add(result);
        }

        return results;
    }

    /**
     * Handles recognition results from the voice recognition engine.
     *
     * @author alanv@google.com (Alan Viverette)
     */
    public static interface GrammarListener {
        /**
         * Called when recognition is complete. Returns results sorted by
         * highest confidence first.
         *
         * @param results a set of results sorted by highest confidence first
         */
        public void onRecognitionSuccess(TreeSet<GrammarResult> results);

        /**
         * Called when recognition fails to return any results. You may resume
         * recognition immediately after receiving this callback.
         */
        public void onRecognitionFailure();

        /**
         * Called when something has gone wrong with recognition. Possible
         * reasons include an unknown event or an exception. You should not
         * resume recognition after this callback.
         *
         * @param reason a String explaining why recognition failed
         */
        public void onRecognitionError(String reason);
    }

    /**
     * A wrapper for GrammarRecognizer recognition results.
     *
     * @author alanv@google.com (Alan Viverette)
     */
    public static class GrammarResult implements Comparable<GrammarResult> {
        private final String meaning;

        private final String literal;

        private final int confidence;

        /**
         * Constructs a new GrammarResult with the given meaning, literal, and
         * confidence.
         *
         * @param meaning the constructed meaning of the literal result
         * @param literal exactly what the speech recognition engine heard
         * @param confidence integer representing the recognition confidence
         */
        public GrammarResult(String meaning, String literal, int confidence) {
            this.meaning = meaning;
            this.literal = literal;
            this.confidence = confidence;
        }

        /**
         * Returns the literal recognition result, which is the grammar-based
         * result heard by the recognizer.
         *
         * @return A String representing what the recognizer heard.
         */
        public String getLiteral() {
            return literal;
        }

        /**
         * Returns the meaning of the recognized literal according to the rules
         * defined in the grammar.
         *
         * @return A String that represents the meaning of the literal result.
         */
        public String getMeaning() {
            return meaning;
        }

        /**
         * Returns the confidence score of the recognition result. Scores are
         * relative to available grammar paths and other returned results.
         *
         * @return An integer value representing the confidence score of the
         *         recognition result.
         */
        public int getConfidence() {
            return confidence;
        }

        /**
         * Returns the difference in confidence between this and another
         * GrammarResult.
         *
         * @param other The other GrammarResult.
         * @return The difference in confidence between the results.
         */
        @Override
        public int compareTo(GrammarResult other) {
            return other.confidence - confidence;
        }
    }

    /**
     * Contains a map of slot names to possible words.
     *
     * @author alanv@google.com (Alan Viverette)
     */
    public static class GrammarMap {
        Map<String, List<GrammarEntry>> slotMap;

        public GrammarMap() {
            slotMap = new HashMap<String, List<GrammarEntry>>();
        }

        public Set<String> getSlots() {
            return slotMap.keySet();
        }

        public Set<Entry<String, List<GrammarEntry>>> getEntries() {
            return slotMap.entrySet();
        }

        public List<GrammarEntry> getSlot(String slot) {
            return slotMap.get(slot);
        }

        /**
         * Assigns a new word to the specified slot. If either word or slot is
         * <code>null</code>, then no-op.
         *
         * @param slot the slot to assign the word to, ex. "Names" (not
         *            "@Names")
         * @param word the word to listen for, ex. "Alan Viverette"
         * @param pron (optional) word pronunciation in SREC format
         * @param weight (optional) word weight relative to other slot words
         * @param tag the meaning assigned to this word
         */
        public void addWord(String slot, String word, String pron, int weight, String tag) {
            if (slot == null || word == null || tag == null) {
                return;
            }

            slot = "@" + slot;
            tag = "V='" + tag + "'";

            List<GrammarEntry> entries = slotMap.get(slot);

            if (entries == null) {
                entries = new LinkedList<GrammarEntry>();
                slotMap.put(slot, entries);
            }

            GrammarEntry entry = new GrammarEntry();
            entry.word = word;
            entry.pron = pron;
            entry.weight = weight;
            entry.tag = tag;

            entries.add(entry);
        }

        /**
         * Assigns each word in the array to the specified slot. If the slot is
         * null, then no-op.
         *
         * @param slot the slot to assign the word to, ex. "Names" (not
         *            "@Names")
         * @param words array of words to listen for, ex. "Alan Viverette"
         * @param prons (optional) array of word pronunciations in SREC format
         * @param weights (optional) array of word weights relative to other
         *            slot word weights; use null for default
         * @param tag the meaning assigned to this word
         */
        public void addWords(
                String slot, String[] words, String[] prons, int[] weights, String tag) {
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                String pron = prons == null ? null : prons[i];
                int weight = weights == null ? 1 : weights[i];

                addWord(slot, word, pron, weight, tag);
            }
        }
    }

    /**
     * Represents a single entry in the grammar map.
     *
     * @author alanv@google.com (Alan Viverette)
     */
    private static class GrammarEntry {
        protected String word;

        protected String pron;

        protected int weight;

        protected String tag;
    }
}
