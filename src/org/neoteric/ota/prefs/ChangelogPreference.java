/*
 * Copyright (C) 2025 Neoteric OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.neoteric.ota.prefs;

import android.content.Context;
import androidx.preference.Preference;
import android.util.AttributeSet;

import org.neoteric.ota.R;

public class ChangelogPreference extends Preference {

    public ChangelogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChangelogPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.pref_changelog);
    }
}
