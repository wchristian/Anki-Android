/***************************************************************************************
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

package com.ichi2.anki;

import com.ichi2.anki.R;

import java.util.HashMap;

public class SharedDeck extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    private int mId;
    private String mTitle;
    private int mFacts;
    private int mSize;
    /**
     * on demand cache for filtering only
     */
    private String mLowerCaseTitle;


    public int getId() {
        return mId;
    }


    public void setId(int id) {
        mId = id;
    }

    public String getTitle() {
        return mTitle;
    }


    public void setTitle(String title) {
        mTitle = title;
        put("title", mTitle);
    }


    public int getFacts() {
        return mFacts;
    }


    public void setFacts(int facts) {
    }


    public int getSize() {
        return mSize;
    }


    public void setSize(int size) {
        mSize = size;
    }

    public boolean matchesLowerCaseFilter(String searchText) {
        // cache our own lower case title, so the next letters in the filter string will be faster
        if (mLowerCaseTitle == null) {
            mLowerCaseTitle = getTitle().toLowerCase();
        }
        return mLowerCaseTitle.contains(searchText);
    }
}
