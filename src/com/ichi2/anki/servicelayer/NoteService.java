/****************************************************************************************
 * Copyright (c) 2013 Bibek Shrestha <bibekshrestha@gmail.com>                          *
 * Copyright (c) 2013 Zaur Molotnikov <qutorial@gmail.com>                              *
 * Copyright (c) 2013 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2013 Flavio Lerda <flerda@gmail.com>                                   *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki.servicelayer;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.multimediacard.IMultimediaEditableNote;
import com.ichi2.anki.multimediacard.fields.AudioField;
import com.ichi2.anki.multimediacard.fields.IField;
import com.ichi2.anki.multimediacard.fields.ImageField;
import com.ichi2.anki.multimediacard.fields.TextField;
import com.ichi2.anki.multimediacard.impl.MultimediaEditableNote;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NoteService {
    /**
     * Creates an empty Note from given Model
     * 
     * @param model the model in JSOBObject format
     * @return a new note instance
     */
    public static MultimediaEditableNote createEmptyNote(JSONObject model) {
        try {
            JSONArray fieldsArray = model.getJSONArray("flds");
            int numOfFields = fieldsArray.length();
            if (numOfFields > 0) {
                MultimediaEditableNote note = new MultimediaEditableNote();
                note.setNumFields(numOfFields);

                for (int i = 0; i < numOfFields; i++) {
                    JSONObject fieldObject = fieldsArray.getJSONObject(i);
                    TextField uiTextField = new TextField();
                    uiTextField.setName(fieldObject.getString("name"));
                    uiTextField.setText(fieldObject.getString("name"));
                    note.setField(i, uiTextField);
                }
                note.setModelId(model.getLong("id"));
                return note;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void updateMultimediaNoteFromJsonNote(final Note editorNoteSrc, final IMultimediaEditableNote noteDst) {
        if (noteDst instanceof MultimediaEditableNote) {
            MultimediaEditableNote mmNote = (MultimediaEditableNote) noteDst;
            String[] values = editorNoteSrc.getFields();
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                IField field = null;
                if (value.startsWith("<img")) {
                    field = new ImageField();
                } else if (value.startsWith("[sound:")) {
                    field = new AudioField();
                } else {
                    field = new TextField();
                }
                field.setFormattedString(value);
                mmNote.setField(i, field);
            }
            mmNote.setModelId(editorNoteSrc.getMid());
        }
    }


    /**
     * Updates the JsonNote field values from MultimediaEditableNote When both notes are using the same Model, it updaes
     * the destination field values with source values. If models are different it throws an Exception
     * 
     * @param mNoteSrc
     * @param mEditorNoteDst
     */
    public static void updateJsonNoteFromMultimediaNote(final IMultimediaEditableNote noteSrc, final Note editorNoteDst) {
        if (noteSrc instanceof MultimediaEditableNote) {
            MultimediaEditableNote mmNote = (MultimediaEditableNote) noteSrc;
            if (mmNote.getModelId() != editorNoteDst.getMid()) {
                throw new RuntimeException("Source and Destination Note ID do not match.");
            }

            int totalFields = mmNote.getNumberOfFields();
            for (int i = 0; i < totalFields; i++) {
                editorNoteDst.values()[i] = mmNote.getField(i).getFormattedValue();
            }
        }
    }


    /**
     * Saves the multimedia associated with this card to proper path inside anki folder. For each field associated with
     * the note it checks for the following condition a. The field content should have changed b. The field content does
     * not already point to a media inside anki media path If both condition satisfies then it copies the file inside
     * the media path and deletes the file referenced by the note
     * 
     * @param note
     */
    public static void saveMedia(final MultimediaEditableNote noteNew) {
        int fieldCount = noteNew.getNumberOfFields();
        for (int i = 0; i < fieldCount; i++) {
            IField newField = noteNew.getField(i);
            importMediaToDirectory(newField);
        }
    }


    /**
     * Considering the field is new, if it has media handle it
     * 
     * @param field
     */
    private static void importMediaToDirectory(IField field) {
        String tmpMediaPath = null;
        switch (field.getType()) {
            case AUDIO:
                tmpMediaPath = field.getAudioPath();
                break;

            case IMAGE:
                tmpMediaPath = field.getImagePath();
                break;

            case TEXT:
            default:
                break;
        }
        if (tmpMediaPath != null) {
            try {
                File inFile = new File(tmpMediaPath);
                if (inFile.exists()) {
                    Collection col = AnkiDroidApp.getCol();
                    String mediaDir = col.getMedia().getDir() + "/";

                    File mediaDirFile = new File(mediaDir);

                    File parent = inFile.getParentFile();

                    // If already there.
                    if (mediaDirFile.getAbsolutePath().contentEquals(parent.getAbsolutePath())) {
                        return;
                    }

                    File outFile = new File(mediaDir + inFile.getName());

                    if (!outFile.exists()) {
                        if (field.hasTemporaryMedia()) {
                            // Move
                            inFile.renameTo(outFile);
                        } else {
                            // Copy
                            InputStream in = new FileInputStream(tmpMediaPath);
                            OutputStream out = new FileOutputStream(outFile.getAbsolutePath());

                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            in.close();
                            out.close();
                        }

                        switch (field.getType()) {
                            case AUDIO:
                                field.setAudioPath(outFile.getAbsolutePath());
                                break;

                            case IMAGE:
                                field.setImagePath(outFile.getAbsolutePath());
                                break;
                            default:
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
