/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2012 Tim Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.installer.console;

import java.nio.charset.Charset;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.util.Console;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Abstract console panel for displaying paginated text.
 *
 * @author Tim Anderson
 */
public abstract class AbstractTextConsolePanel extends AbstractConsolePanel
{

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(AbstractTextConsolePanel.class.getName());

    /**
     * Constructs an {@code AbstractTextConsolePanel}.
     *
     * @param panel the parent panel/view. May be {@code null}
     */
    public AbstractTextConsolePanel(PanelView<ConsolePanel> panel)
    {
        super(panel);
    }

    /**
     * Runs the panel using the supplied properties.
     *
     * @param installData the installation data
     * @param properties  the properties
     * @return <tt>true</tt>
     */
    @Override
    public boolean run(InstallData installData, Properties properties)
    {
        return true;
    }

    /**
     * Runs the panel using the specified console.
     * <p/>
     * If there is no text to display, the panel will return <tt>false</tt>.
     *
     * @param installData the installation data
     * @param console     the console
     * @return <tt>true</tt> if the panel ran successfully, otherwise <tt>false</tt>
     */
    @Override
    public boolean run(InstallData installData, Console console)
    {
        printHeadLine(installData, console);

        String text = getText();
        text = installData.getVariables().replace(text);
        if (text != null)
        {
            Panel panel = getPanel();
            RulesEngine rules = installData.getRules();
            boolean paging = Boolean.parseBoolean(panel.getConfigurationOptionValue("console-text-paging", rules));
            boolean wordwrap = Boolean.parseBoolean(panel.getConfigurationOptionValue("console-text-wordwrap", rules));

            String platform = installData.getVariable("platform");
            if (platform != null && platform.equalsIgnoreCase("windows")) {
                text = new String(text.getBytes(Charset.forName("Cp850")));
            }

            try
            {
                console.printMultiLine(text, wordwrap, paging);
            }
            catch (IOException e)
            {
                logger.warning("Displaying multiline text failed: " + e.getMessage());
            }
        }
        else
        {
            logger.warning("No text to display");
        }
        return promptEndPanel(installData, console);
    }

    /**
     * Returns the text to display.
     *
     * @return the text. A <tt>null</tt> indicates failure
     */
    protected abstract String getText();

    /**
     * Helper to strip HTML from text.
     * From code originally developed by Jan Blok.
     *
     * @param text the text. May be {@code null}
     * @return the text with HTML removed
     */
    protected String removeHTML(String text)
    {
        String result = "";

        if (text != null)
        {
            // chose to keep newline (\n) instead of carriage return (\r) for line breaks.

            // Replace line breaks with space
            result = text.replaceAll("\r", " ");
            // Remove step-formatting
            result = result.replaceAll("\t", "");
            // Remove repeating spaces because browsers ignore them

            result = result.replaceAll("( )+", " ");


            result = result.replaceAll("<( )*head([^>])*>", "<head>");
            result = result.replaceAll("(<( )*(/)( )*head( )*>)", "</head>");
            result = result.replaceAll("(<head>).*(</head>)", "");
            result = result.replaceAll("<( )*script([^>])*>", "<script>");
            result = result.replaceAll("(<( )*(/)( )*script( )*>)", "</script>");
            result = result.replaceAll("(<script>).*(</script>)", "");

            // remove all styles (prepare first by clearing attributes)
            result = result.replaceAll("<( )*style([^>])*>", "<style>");
            result = result.replaceAll("(<( )*(/)( )*style( )*>)", "</style>");
            result = result.replaceAll("(<style>).*(</style>)", "");

            result = result.replaceAll("(<( )*(/)( )*sup( )*>)", "</sup>");
            result = result.replaceAll("<( )*sup([^>])*>", "<sup>");
            result = result.replaceAll("(<sup>).*(</sup>)", "");

            // insert tabs in spaces of <td> tags
            result = result.replaceAll("<( )*td([^>])*>", "\t");

            // insert line breaks in places of <BR> and <LI> tags
            result = result.replaceAll("<( )*br( )*>", "\r");
            result = result.replaceAll("<( )*li( )*>", "\r");

            // insert line paragraphs (double line breaks) in place
            // if <P>, <DIV> and <TR> tags
            result = result.replaceAll("<( )*div([^>])*>", "\r\r");
            result = result.replaceAll("<( )*tr([^>])*>", "\r\r");

            result = result.replaceAll("(<) h (\\w+) >", "\r");
            result = result.replaceAll("(\\b) (</) h (\\w+) (>) (\\b)", "");
            result = result.replaceAll("<( )*p([^>])*>", "\r\r");

            // Remove remaining tags like <a>, links, images,
            // comments etc - anything that's enclosed inside < >
            result = result.replaceAll("<[^>]*>", "");


            result = result.replaceAll("&bull;", " * ");
            result = result.replaceAll("&lsaquo;", "<");
            result = result.replaceAll("&rsaquo;", ">");
            result = result.replaceAll("&trade;", "(tm)");
            result = result.replaceAll("&frasl;", "/");
            result = result.replaceAll("&lt;", "<");
            result = result.replaceAll("&gt;", ">");

            result = result.replaceAll("&copy;", "(c)");
            result = result.replaceAll("&reg;", "(r)");
            result = replaceAllEntities(result);
            result = result.replaceAll("&(.{2,6});", "");

            // Remove extra line breaks and tabs:
            // replace over 2 breaks with 2 and over 4 tabs with 4.
            // Prepare first to remove any whitespaces in between
            // the escaped characters and remove redundant tabs in between line breaks
            result = result.replaceAll("(\r)( )+(\r)", "\r\r");
            result = result.replaceAll("(\t)( )+(\t)", "\t\t");
            result = result.replaceAll("(\t)( )+(\r)", "\t\r");
            result = result.replaceAll("(\r)( )+(\t)", "\r\t");
            result = result.replaceAll("(\r)(\t)+(\\r)", "\r\r");
            result = result.replaceAll("(\r)(\t)+", "\r\t");
        }
        return result;
    }

    private final String replaceAllEntities(final String str) {
        String result = str;
        //
        result = result.replaceAll("&quot;", "\""); // quotation mark (APL quote)
        result = result.replaceAll("&amp;", "\u0026"); // ampersand
        result = result.replaceAll("&apos;", "\u0027"); // apostrophe (apostrophe-quote); see below
        result = result.replaceAll("&lt;", "\u003C"); // less-than sign
        result = result.replaceAll("&gt;", "\u003E"); // greater-than sign
        result = result.replaceAll("&nbsp;", "\u00A0"); // no-break space (non-breaking space)[d]
        result = result.replaceAll("&iexcl;", "\u00A1"); // inverted exclamation mark
        result = result.replaceAll("&cent;", "\u00A2"); // cent sign
        result = result.replaceAll("&pound;", "\u00A3"); // pound sign
        result = result.replaceAll("&curren;", "\u00A4"); // currency sign
        result = result.replaceAll("&yen;", "\u00A5"); // yen sign (yuan sign)
        result = result.replaceAll("&brvbar;", "\u00A6"); // broken bar (broken vertical bar)
        result = result.replaceAll("&sect;", "\u00A7"); // section sign
        result = result.replaceAll("&uml;", "\u00A8"); // diaeresis (spacing diaeresis); see Germanic umlaut
        result = result.replaceAll("&copy;", "\u00A9"); // copyright symbol
        result = result.replaceAll("&ordf;", "\u00AA"); // feminine ordinal indicator
        result = result.replaceAll("&laquo;", "\u00AB"); // left-pointing double angle quotation mark (left pointingguillemet)
        result = result.replaceAll("&not;", "\u00AC"); // not sign
        result = result.replaceAll("&shy;", "\u00AD"); // soft hyphen (discretionary hyphen)
        result = result.replaceAll("&reg;", "\u00AE"); // registered sign (registered trademark symbol)
        result = result.replaceAll("&macr;", "\u00AF"); // macron (spacing macron, overline, APL overbar)
        result = result.replaceAll("&deg;", "\u00B0"); // degree symbol
        result = result.replaceAll("&plusmn;", "\u00B1"); // plus-minus sign (plus-or-minus sign)
        result = result.replaceAll("&sup2;", "\u00B2"); // superscript two (superscript digit two, squared)
        result = result.replaceAll("&sup3;", "\u00B3"); // superscript three (superscript digit three, cubed)
        result = result.replaceAll("&acute;", "\u00B4"); // acute accent (spacing acute)
        result = result.replaceAll("&micro;", "\u00B5"); // micro sign
        result = result.replaceAll("&para;", "\u00B6"); // pilcrow sign (paragraph sign)
        result = result.replaceAll("&middot;", "\u00B7"); // middle dot (Georgian comma, Greek middle dot)
        result = result.replaceAll("&cedil;", "\u00B8"); // cedilla (spacing cedilla)
        result = result.replaceAll("&sup1;", "\u00B9"); // superscript one (superscript digit one)
        result = result.replaceAll("&ordm;", "\u00BA"); // masculine ordinal indicator
        result = result.replaceAll("&raquo;", "\u00BB"); // right-pointing double angle quotation mark (right pointing guillemet)
        result = result.replaceAll("&frac14;", "\u00BC"); // vulgar fraction one quarter (fraction one quarter)
        result = result.replaceAll("&frac12;", "\u00BD"); // vulgar fraction one half (fraction one half)
        result = result.replaceAll("&frac34;", "\u00BE"); // vulgar fraction three quarters (fraction three quarters)
        result = result.replaceAll("&iquest;", "\u00BF"); // inverted question mark (turned question mark)
        result = result.replaceAll("&Agrave;", "\u00C0"); // Latin capital letter A with grave accent (Latin capital letter
                                                          // Acapital letter Agrave)
        result = result.replaceAll("&Aacute;", "\u00C1"); // Latin capital letter A with acute accent
        result = result.replaceAll("&Acirc;", "\u00C2"); // Latin capital letter A with circumflex
        result = result.replaceAll("&Atilde;", "\u00C3"); // Latin capital letter A with tilde
        result = result.replaceAll("&Auml;", "\u00C4"); // Latin capital letter A with diaeresis
        result = result.replaceAll("&Aring;", "\u00C5"); // Latin capital letter A with ring above (Latin capital letter A ring)
        result = result.replaceAll("&AElig;", "\u00C6"); // Latin capital letter AE (Latin capital ligature AE)
        result = result.replaceAll("&Ccedil;", "\u00C7"); // Latin capital letter C with cedilla
        result = result.replaceAll("&Egrave;", "\u00C8"); // Latin capital letter E with grave accent
        result = result.replaceAll("&Eacute;", "\u00C9"); // Latin capital letter E with acute accent
        result = result.replaceAll("&Ecirc;", "\u00CA"); // Latin capital letter E with circumflex
        result = result.replaceAll("&Euml;", "\u00CB"); // Latin capital letter E with diaeresis
        result = result.replaceAll("&Igrave;", "\u00CC"); // Latin capital letter I with grave accent
        result = result.replaceAll("&Iacute;", "\u00CD"); // Latin capital letter I with acute accent
        result = result.replaceAll("&Icirc;", "\u00CE"); // Latin capital letter I with circumflex
        result = result.replaceAll("&Iuml;", "\u00CF"); // Latin capital letter I with diaeresis
        result = result.replaceAll("&ETH;", "\u00D0"); // Latin capital letter Eth
        result = result.replaceAll("&Ntilde;", "\u00D1"); // Latin capital letter N with tilde
        result = result.replaceAll("&Ograve;", "\u00D2"); // Latin capital letter O with grave accent
        result = result.replaceAll("&Oacute;", "\u00D3"); // Latin capital letter O with acute accent
        result = result.replaceAll("&Ocirc;", "\u00D4"); // Latin capital letter O with circumflex
        result = result.replaceAll("&Otilde;", "\u00D5"); // Latin capital letter O with tilde
        result = result.replaceAll("&Ouml;", "\u00D6"); // Latin capital letter O with diaeresis
        result = result.replaceAll("&times;", "\u00D7"); // multiplication sign
        result = result.replaceAll("&Oslash;", "\u00D8"); // Latin capital letter O with stroke (Latin capital letter O slash)
        result = result.replaceAll("&Ugrave;", "\u00D9"); // Latin capital letter U with grave accent
        result = result.replaceAll("&Uacute;", "\u00DA"); // Latin capital letter U with acute accent
        result = result.replaceAll("&Ucirc;", "\u00DB"); // Latin capital letter U with circumflex
        result = result.replaceAll("&Uuml;", "\u00DC"); // Latin capital letter U with diaeresis
        result = result.replaceAll("&Yacute;", "\u00DD"); // Latin capital letter Y with acute accent
        result = result.replaceAll("&THORN;", "\u00DE"); // Latin capital letter THORN
        result = result.replaceAll("&szlig;", "\u00DF"); // Latin small letter sharp s (ess-zed); see German Eszett
        result = result.replaceAll("&agrave;", "\u00E0"); // Latin small letter a with grave accent
        result = result.replaceAll("&aacute;", "\u00E1"); // Latin small letter a with acute accent
        result = result.replaceAll("&acirc;", "\u00E2"); // Latin small letter a with circumflex
        result = result.replaceAll("&atilde;", "\u00E3"); // Latin small letter a with tilde
        result = result.replaceAll("&auml;", "\u00E4"); // Latin small letter a with diaeresis
        result = result.replaceAll("&aring;", "\u00E5"); // Latin small letter a with ring above
        result = result.replaceAll("&aelig;", "\u00E6"); // Latin small letter ae (Latin small ligature ae)
        result = result.replaceAll("&ccedil;", "\u00E7"); // Latin small letter c with cedilla
        result = result.replaceAll("&egrave;", "\u00E8"); // Latin small letter e with grave accent
        result = result.replaceAll("&eacute;", "\u00E9"); // Latin small letter e with acute accent
        result = result.replaceAll("&ecirc;", "\u00EA"); // Latin small letter e with circumflex
        result = result.replaceAll("&euml;", "\u00EB"); // Latin small letter e with diaeresis
        result = result.replaceAll("&igrave;", "\u00EC"); // Latin small letter i with grave accent
        result = result.replaceAll("&iacute;", "\u00ED"); // Latin small letter i with acute accent
        result = result.replaceAll("&icirc;", "\u00EE"); // Latin small letter i with circumflex
        result = result.replaceAll("&iuml;", "\u00EF"); // Latin small letter i with diaeresis
        result = result.replaceAll("&eth;", "\u00F0"); // Latin small letter eth
        result = result.replaceAll("&ntilde;", "\u00F1"); // Latin small letter n with tilde
        result = result.replaceAll("&ograve;", "\u00F2"); // Latin small letter o with grave accent
        result = result.replaceAll("&oacute;", "\u00F3"); // Latin small letter o with acute accent
        result = result.replaceAll("&ocirc;", "\u00F4"); // Latin small letter o with circumflex
        result = result.replaceAll("&otilde;", "\u00F5"); // Latin small letter o with tilde
        result = result.replaceAll("&ouml;", "\u00F6"); // Latin small letter o with diaeresis
        result = result.replaceAll("&divide;", "\u00F7"); // division sign (obelus)
        result = result.replaceAll("&oslash;", "\u00F8"); // Latin small letter o with stroke (Latin small letter o slash)
        result = result.replaceAll("&ugrave;", "\u00F9"); // Latin small letter u with grave accent
        result = result.replaceAll("&uacute;", "\u00FA"); // Latin small letter u with acute accent
        result = result.replaceAll("&ucirc;", "\u00FB"); // Latin small letter u with circumflex
        result = result.replaceAll("&uuml;", "\u00FC"); // Latin small letter u with diaeresis
        result = result.replaceAll("&yacute;", "\u00FD"); // Latin small letter y with acute accent
        result = result.replaceAll("&thorn;", "\u00FE"); // Latin small letter thorn
        result = result.replaceAll("&yuml;", "\u00FF"); // Latin small letter y with diaeresis
        result = result.replaceAll("&OElig;", "\u0152"); // Latin capital ligature oe[e]
        result = result.replaceAll("&oelig;", "\u0153"); // Latin small ligature oe[e]
        result = result.replaceAll("&Scaron;", "\u0160"); // Latin capital letter s with caron
        result = result.replaceAll("&scaron;", "\u0161"); // Latin small letter s with caron
        result = result.replaceAll("&Yuml;", "\u0178"); // Latin capital letter y with diaeresis
        result = result.replaceAll("&fnof;", "\u0192"); // Latin small letter f with hook (function, florin)
        result = result.replaceAll("&circ;", "\u02C6"); // modifier letter circumflex accent
        result = result.replaceAll("&tilde;", "\u02DC"); // small tilde
        result = result.replaceAll("&Alpha;", "\u0391"); // Greek capital letter Alpha
        result = result.replaceAll("&Beta;", "\u0392"); // Greek capital letter Beta
        result = result.replaceAll("&Gamma;", "\u0393"); // Greek capital letter Gamma
        result = result.replaceAll("&Delta;", "\u0394"); // Greek capital letter Delta
        result = result.replaceAll("&Epsilon;", "\u0395"); // Greek capital letter Epsilon
        result = result.replaceAll("&Zeta;", "\u0396"); // Greek capital letter Zeta
        result = result.replaceAll("&Eta;", "\u0397"); // Greek capital letter Eta
        result = result.replaceAll("&Theta;", "\u0398"); // Greek capital letter Theta
        result = result.replaceAll("&Iota;", "\u0399"); // Greek capital letter Iota
        result = result.replaceAll("&Kappa;", "\u039A"); // Greek capital letter Kappa
        result = result.replaceAll("&Lambda;", "\u039B"); // Greek capital letter Lambda
        result = result.replaceAll("&Mu;", "\u039C"); // Greek capital letter Mu
        result = result.replaceAll("&Nu;", "\u039D"); // Greek capital letter Nu
        result = result.replaceAll("&Xi;", "\u039E"); // Greek capital letter Xi
        result = result.replaceAll("&Omicron;", "\u039F"); // Greek capital letter Omicron
        result = result.replaceAll("&Pi;", "\u03A0"); // Greek capital letter Pi
        result = result.replaceAll("&Rho;", "\u03A1"); // Greek capital letter Rho
        result = result.replaceAll("&Sigma;", "\u03A3"); // Greek capital letter Sigma
        result = result.replaceAll("&Tau;", "\u03A4"); // Greek capital letter Tau
        result = result.replaceAll("&Upsilon;", "\u03A5"); // Greek capital letter Upsilon
        result = result.replaceAll("&Phi;", "\u03A6"); // Greek capital letter Phi
        result = result.replaceAll("&Chi;", "\u03A7"); // Greek capital letter Chi
        result = result.replaceAll("&Psi;", "\u03A8"); // Greek capital letter Psi
        result = result.replaceAll("&Omega;", "\u03A9"); // Greek capital letter Omega
        result = result.replaceAll("&alpha;", "\u03B1"); // Greek small letter alpha
        result = result.replaceAll("&beta;", "\u03B2"); // Greek small letter beta
        result = result.replaceAll("&gamma;", "\u03B3"); // Greek small letter gamma
        result = result.replaceAll("&delta;", "\u03B4"); // Greek small letter delta
        result = result.replaceAll("&epsilon;", "\u03B5"); // Greek small letter epsilon
        result = result.replaceAll("&zeta;", "\u03B6"); // Greek small letter zeta
        result = result.replaceAll("&eta;", "\u03B7"); // Greek small letter eta
        result = result.replaceAll("&theta;", "\u03B8"); // Greek small letter theta
        result = result.replaceAll("&iota;", "\u03B9"); // Greek small letter iota
        result = result.replaceAll("&kappa;", "\u03BA"); // Greek small letter kappa
        result = result.replaceAll("&lambda;", "\u03BB"); // Greek small letter lambda
        result = result.replaceAll("&mu;", "\u03BC"); // Greek small letter mu
        result = result.replaceAll("&nu;", "\u03BD"); // Greek small letter nu
        result = result.replaceAll("&xi;", "\u03BE"); // Greek small letter xi
        result = result.replaceAll("&omicron;", "\u03BF"); // Greek small letter omicron
        result = result.replaceAll("&pi;", "\u03C0"); // Greek small letter pi
        result = result.replaceAll("&rho;", "\u03C1"); // Greek small letter rho
        result = result.replaceAll("&sigmaf;", "\u03C2"); // Greek small letter final sigma
        result = result.replaceAll("&sigma;", "\u03C3"); // Greek small letter sigma
        result = result.replaceAll("&tau;", "\u03C4"); // Greek small letter tau
        result = result.replaceAll("&upsilon;", "\u03C5"); // Greek small letter upsilon
        result = result.replaceAll("&phi;", "\u03C6"); // Greek small letter phi
        result = result.replaceAll("&chi;", "\u03C7"); // Greek small letter chi
        result = result.replaceAll("&psi;", "\u03C8"); // Greek small letter psi
        result = result.replaceAll("&omega;", "\u03C9"); // Greek small letter omega
        result = result.replaceAll("&thetasym;", "\u03D1"); // Greek theta symbol
        result = result.replaceAll("&upsih;", "\u03D2"); // Greek Upsilon with hook symbol
        result = result.replaceAll("&piv;", "\u03D6"); // Greek pi symbol
        result = result.replaceAll("&ensp;", "\u2002"); // en space[d]
        result = result.replaceAll("&emsp;", "\u2003"); // em space[d]
        result = result.replaceAll("&thinsp;", "\u2009"); // thin space[d]
        result = result.replaceAll("&zwnj;", "\u200C"); // zero-width non-joiner
        result = result.replaceAll("&zwj;", "\u200D"); // zero-width joiner
        result = result.replaceAll("&lrm;", "\u200E"); // left-to-right mark
        result = result.replaceAll("&rlm;", "\u200F"); // right-to-left mark
        result = result.replaceAll("&ndash;", "\u2013"); // en dash
        result = result.replaceAll("&mdash;", "\u2014"); // em dash
        result = result.replaceAll("&lsquo;", "\u2018"); // left single quotation mark
        result = result.replaceAll("&rsquo;", "\u2019"); // right single quotation mark
        result = result.replaceAll("&sbquo;", "\u201A"); // single low-9 quotation mark
        result = result.replaceAll("&ldquo;", "\u201C"); // left double quotation mark
        result = result.replaceAll("&rdquo;", "\u201D"); // right double quotation mark
        result = result.replaceAll("&bdquo;", "\u201E"); // double low-9 quotation mark
        result = result.replaceAll("&dagger;", "\u2020"); // dagger, obelisk
        result = result.replaceAll("&Dagger;", "\u2021"); // double dagger, double obelisk
        result = result.replaceAll("&bull;", "\u2022"); // bullet (black small circle)[f]
        result = result.replaceAll("&hellip;", "\u2026"); // horizontal ellipsis (three dot leader)
        result = result.replaceAll("&permil;", "\u2030"); // per mille sign
        result = result.replaceAll("&prime;", "\u2032"); // prime (minutes, feet)
        result = result.replaceAll("&Prime;", "\u2033"); // double prime (seconds, inches)
        result = result.replaceAll("&lsaquo;", "\u2039"); // single left-pointing angle quotation mark[g]
        result = result.replaceAll("&rsaquo;", "\u203A"); // single right-pointing angle quotation mark[g]
        result = result.replaceAll("&oline;", "\u203E"); // overline (spacing overscore)
        result = result.replaceAll("&frasl;", "\u2044"); // fraction slash (solidus)
        result = result.replaceAll("&euro;", "\u20AC"); // euro sign
        result = result.replaceAll("&image;", "\u2111"); // black-letter capital I (imaginary part)
        result = result.replaceAll("&weierp;", "\u2118"); // script capital P (power set, Weierstrass p)
        result = result.replaceAll("&real;", "\u211C"); // black-letter capital R (real part symbol)
        result = result.replaceAll("&trade;", "\u2122"); // trademark symbol
        result = result.replaceAll("&alefsym;", "\u2135"); // alef symbol (first transfinite cardinal)[h]
        result = result.replaceAll("&larr;", "\u2190"); // leftwards arrow
        result = result.replaceAll("&uarr;", "\u2191"); // upwards arrow
        result = result.replaceAll("&rarr;", "\u2192"); // rightwards arrow
        result = result.replaceAll("&darr;", "\u2193"); // downwards arrow
        result = result.replaceAll("&harr;", "\u2194"); // left right arrow
        result = result.replaceAll("&crarr;", "\u21B5"); // downwards arrow with corner leftwards (carriage return)
        result = result.replaceAll("&lArr;", "\u21D0"); // leftwards double arrow[i]
        result = result.replaceAll("&uArr;", "\u21D1"); // upwards double arrow
        result = result.replaceAll("&rArr;", "\u21D2"); // rightwards double arrow[j]
        result = result.replaceAll("&dArr;", "\u21D3"); // downwards double arrow
        result = result.replaceAll("&hArr;", "\u21D4"); // left right double arrow
        result = result.replaceAll("&forall;", "\u2200"); // for all
        result = result.replaceAll("&part;", "\u2202"); // partial differential
        result = result.replaceAll("&exist;", "\u2203"); // there exists
        result = result.replaceAll("&empty;", "\u2205"); // empty set (null set); see also U+8960, ⌀
        result = result.replaceAll("&nabla;", "\u2207"); // del or nabla (vector differential operator)
        result = result.replaceAll("&isin;", "\u2208"); // element of
        result = result.replaceAll("&notin;", "\u2209"); // not an element of
        result = result.replaceAll("&ni;", "\u220B"); // contains as member
        result = result.replaceAll("&prod;", "\u220F"); // n-ary product (product sign)[k]
        result = result.replaceAll("&sum;", "\u2211"); // n-ary summation[l]
        result = result.replaceAll("&minus;", "\u2212"); // minus sign
        result = result.replaceAll("&lowast;", "\u2217"); // asterisk operator
        result = result.replaceAll("&radic;", "\u221A"); // square root (radical sign)
        result = result.replaceAll("&prop;", "\u221D"); // proportional to
        result = result.replaceAll("&infin;", "\u221E"); // infinity
        result = result.replaceAll("&ang;", "\u2220"); // angle
        result = result.replaceAll("&and;", "\u2227"); // logical and (wedge)
        result = result.replaceAll("&or;", "\u2228"); // logical or (vee)
        result = result.replaceAll("&cap;", "\u2229"); // intersection (cap)
        result = result.replaceAll("&cup;", "\u222A"); // union (cup)
        result = result.replaceAll("&int;", "\u222B"); // integral
        result = result.replaceAll("&there4;", "\u2234"); // therefore sign
        result = result.replaceAll("&sim;", "\u223C"); // tilde operator (varies with, similar to)[m]
        result = result.replaceAll("&cong;", "\u2245"); // congruent to
        result = result.replaceAll("&asymp;", "\u2248"); // almost equal to (asymptotic to)
        result = result.replaceAll("&ne;", "\u2260"); // not equal to
        result = result.replaceAll("&equiv;", "\u2261"); // identical to; sometimes used for 'equivalent to'
        result = result.replaceAll("&le;", "\u2264"); // less-than or equal to
        result = result.replaceAll("&ge;", "\u2265"); // greater-than or equal to
        result = result.replaceAll("&sub;", "\u2282"); // subset of
        result = result.replaceAll("&sup;", "\u2283"); // superset of[n]
        result = result.replaceAll("&nsub;", "\u2284"); // not a subset of
        result = result.replaceAll("&sube;", "\u2286"); // subset of or equal to
        result = result.replaceAll("&supe;", "\u2287"); // superset of or equal to
        result = result.replaceAll("&oplus;", "\u2295"); // circled plus (direct sum)
        result = result.replaceAll("&otimes;", "\u2297"); // circled times (vector product)
        result = result.replaceAll("&perp;", "\u22A5"); // up tack (orthogonal to, perpendicular)[o]
        result = result.replaceAll("&sdot;", "\u22C5"); // dot operator[p]
        result = result.replaceAll("&vellip;", "\u22EE"); // vertical ellipsis
        result = result.replaceAll("&lceil;", "\u2308"); // left ceiling (APL upstile)
        result = result.replaceAll("&rceil;", "\u2309"); // right ceiling
        result = result.replaceAll("&lfloor;", "\u230A"); // left floor (APL downstile)
        result = result.replaceAll("&rfloor;", "\u230B"); // right floor
        result = result.replaceAll("&lang;", "\u2329"); // left-pointing angle bracket (bra)[q]
        result = result.replaceAll("&rang;", "\u232A"); // right-pointing angle bracket (ket)[r]
        result = result.replaceAll("&loz;", "\u25CA"); // lozenge
        result = result.replaceAll("&spades;", "\u2660"); // black spade suit[f]
        result = result.replaceAll("&clubs;", "\u2663"); // black club suit (shamrock)[f]
        result = result.replaceAll("&hearts;", "\u2665"); // black heart suit (valentine)[f]
        result = result.replaceAll("&diams;", "\u2666"); // black diamond suit[f]
        //
        return result;
    }

}
