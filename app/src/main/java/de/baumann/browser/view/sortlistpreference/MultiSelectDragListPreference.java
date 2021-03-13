package de.baumann.browser.view.sortlistpreference;

/*
 * Original code from Android MultiSelectListPreference
 * Modified to add a drag and drop behavior
 *
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceManager;

import de.baumann.browser.Ninja.R;

public class MultiSelectDragListPreference extends DialogPreference
{
    public CharSequence[] entries;
    public CharSequence[] entryValues;
    public List<String> values = new ArrayList<>();
    public List<String> newValues = new ArrayList<>();

    public MultiSelectDragListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MultiSelectDragListPreference, 0, 0);
        entries = a.getTextArray(R.styleable.MultiSelectDragListPreference_entries);
        entryValues = a.getTextArray(R.styleable.MultiSelectDragListPreference_entryValues);
        a.recycle();
    }

    public MultiSelectDragListPreference(Context context)
    {
        this(context, null);
    }

    /**
     * Sets the human-readable entries to be shown in the list. This will be
     * shown in subsequent dialogs.
     * <p>
     * Each entry must have a corresponding index in
     * {@link #setEntryValues(CharSequence[])}.
     *
     * @param entries The entries.
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntries(CharSequence[] entries)
    {
        this.entries = entries;
    }

    /**
     * @see #setEntries(CharSequence[])
     * @param entriesResId The entries array as a resource.
     */
    public void setEntries(int entriesResId)
    {
        setEntries(getContext().getResources().getTextArray(entriesResId));
    }

    /**
     * The list of entries to be shown in the list in subsequent dialogs.
     *
     * @return The list as an array.
     */
    public CharSequence[] getEntries()
    {
        return entries;
    }

    /**
     * The array to find the value to save for a preference when an entry from
     * entries is selected. If a user clicks on the second item in entries, the
     * second item in this array will be saved to the preference.
     *
     * @param entryValues The array to be used as values to save for the
     *            preference.
     */
    public void setEntryValues(CharSequence[] entryValues)
    {
        this.entryValues = entryValues;
    }

    /**
     * @see #setEntryValues(CharSequence[])
     * @param entryValuesResId The entry values array as a resource.
     */
    public void setEntryValues(int entryValuesResId)
    {
        setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
    }

    public void setValues(List<String> values)
    {
        this.values.clear();
        this.values.addAll(values);

        persistStringSet(values);
    }

    protected boolean  persistStringSet(List<String> values)
    {
        if (shouldPersist())
        {
            // Shouldn't store null
            if (values.equals(getPersistedStringSet("")))
            {
                // It's already there, so the same as persisting
                return true;
            }

            SharedPreferences.Editor editor = mPreferenceManager.getSharedPreferences().edit();
            editor.putString(getKey(), TextUtils.join(",", values));
            editor.commit();
            return true;
        }
        return false;
    }

    /**
     * Checks whether, at the given time this method is called,
     * this Preference should store/restore its value(s) into the
     * {@link SharedPreferences}. This, at minimum, checks whether this
     * Preference is persistent and it currently has a key. Before you
     * save/restore from the {@link SharedPreferences}, check this first.
     *
     * @return True if it should persist the value.
     */
    protected boolean  shouldPersist()
    {
        return mPreferenceManager != null && isPersistent() && hasKey();
    }

    /**
     * Retrieves the current value of the key.
     */
    public List<String> getValues()
    {
        return values;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        final CharSequence[] defaultValues = a.getTextArray(index);
        final int valueCount = defaultValues.length;
        final List<String> result = new ArrayList<String>();

        for (int i = 0; i < valueCount; i++)
        {
            result.add(defaultValues[i].toString());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetInitialValue(boolean  restoreValue, Object defaultValue)
    {
        List<String> v = (List<String>) defaultValue;
        String defValue = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getKey(), TextUtils.join(",", v.toArray()));
        setValues(restoreValue ? getPersistedStringSet(TextUtils.join(",", values)) : Arrays.asList(defValue.split(",")));
    }

    PreferenceManager mPreferenceManager;

    protected List<String> getPersistedStringSet(String defaultReturnValue)
    {
        if (!shouldPersist())
        {
            return Arrays.asList(defaultReturnValue.split(","));
        }

        return Arrays.asList(mPreferenceManager.getSharedPreferences().getString(getKey(), defaultReturnValue)
                .split(","));
    }

    protected void onAttachedToHierarchy(PreferenceManager preferenceManager)
    {
        super.onAttachedToHierarchy(preferenceManager);
        mPreferenceManager = preferenceManager;
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent())
        {
            // No need to save instance state
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.values = TextUtils.join(",", getValues());
        return myState;
    }

    private static class SavedState extends BaseSavedState
    {
        String	values;

        public SavedState(Parcel source)
        {
            super(source);
            values = source.readString();
        }

        public SavedState(Parcelable superState)
        {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            super.writeToParcel(dest, flags);
            dest.writeString(values);
        }

        public static final Parcelable.Creator<SavedState>	CREATOR	= new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}