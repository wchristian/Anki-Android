/***************************************************************************************
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
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

package com.ichi2.libanki;

import com.ichi2.libanki.hooks.Hook;
import com.ichi2.libanki.hooks.Hooks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class used to display and handle correctly LaTeX.
 */
public class LaTeX {
    public class LaTeXFilter extends Hook {
        @Override
        public Object runFilter(Object arg, Object... args) {
            return LaTeX.mungeQA((String) arg, (Collection) args[4]);
        }
    }


    public void installHook(Hooks h) {
        h.addHook("mungeQA", new LaTeXFilter());
    }

    /**
     * Patterns used to identify LaTeX tags
     */
    public static Pattern sStandardPattern = Pattern.compile("\\[latex\\](.+?)\\[/latex\\]");
    public static Pattern sExpressionPattern = Pattern.compile("\\[\\$\\](.+?)\\[/\\$\\]");
    public static Pattern sMathPattern = Pattern.compile("\\[\\$\\$\\](.+?)\\[/\\$\\$\\]");
    public static Pattern sEntityPattern = Pattern.compile("(&[a-z]+;)");


    /**
     * Convert TEXT with embedded latex tags to image links.
     * 
     * @param html The content to search for embedded latex tags.
     * @param col The related collection.
     * @return The content with the tags converted to links.
     */
    public static String mungeQA(String html, Collection col) {
        StringBuffer sb = new StringBuffer();

        Matcher matcher = sStandardPattern.matcher(html);
        while (matcher.find()) {
            matcher.appendReplacement(sb, _imgLink(col, matcher.group(1)));
        }
        matcher.appendTail(sb);

        matcher = sExpressionPattern.matcher(sb.toString());
        sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, _imgLink(col, "$" + matcher.group(1) + "$"));
        }
        matcher.appendTail(sb);

        matcher = sMathPattern.matcher(sb.toString());
        sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    _imgLink(col, "\\begin{displaymath}" + matcher.group(1) + "\\end{displaymath}"));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }


    /**
     * Return an img link for LATEX, creating it if necessary.
     * 
     * @param col The associated Collection object.
     * @param latex The LATEX expression to be replaced
     * @return A string with the link to the image that is the representation of the LATEX expression.
     */
    private static String _imgLink(Collection col, String latex) {
        String txt = _latexFromHtml(col, latex);
        String fname = "latex-" + Utils.checksum(txt) + ".png";
        String link = "<img src=\"" + fname + "\">";
        return link;
    }


    /**
     * Convert entities and fix newlines.
     * 
     * @param col The associated Collection where the LATEX is found
     * @param latex The
     * @return
     */
    private static String _latexFromHtml(Collection col, String latex) {
        latex = latex.replaceAll("<br( /)?>|<div>", "\n");
        latex = Utils.stripHTML(latex);
        return latex;
    }
}
