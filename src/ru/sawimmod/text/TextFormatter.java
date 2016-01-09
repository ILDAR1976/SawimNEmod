package ru.sawimmod.text;

import android.text.*;
import android.text.style.ImageSpan;
import protocol.Contact;
import ru.sawimmod.Scheme;
import ru.sawimmod.modules.Emotions;
import ru.sawimmod.view.menu.JuickMenu;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 19.06.13
 * Time: 20:45
 * To change this template use File | Settings | File Templates.
 */
public class TextFormatter {

    private Pattern juickPattern = Pattern.compile("(#[0-9]+(/[0-9]+)?)");
    private Pattern pstoPattern = Pattern.compile("(#[\\w]+(/[0-9]+)?)");
    private Pattern nickPattern = Pattern.compile("(@[\\w@.-]+(/[0-9]+)?)");
    private Pattern urlPattern = Pattern.compile("(([^ @/<>'\\\"]+)@([^ @/<>'\\\"]+)\\.([a-zA-Z\\.]{2,6})(?:/([^ <>'\\\"]*))?)|(((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?((?:(?:[a-zA-Z0-9а-яА-Я][a-zA-Zа-яА-Я0-9\\-]{0,64}\\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:inc|info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnprwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eosuw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agksyz]|v[aceginu]|w[fs]|(?:рф|xxx)|y[et]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\\:\\d{1,5})?)(\\/(?:(?:[a-zA-Zа-яА-Я0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?(?:\\b|$))");
    private Pattern smilesPattern;
    private static final Emotions smiles = Emotions.instance;
    private static TextFormatter instance;

    public static void init() {
        if (instance == null)
            instance = new TextFormatter();
    }

    public static TextFormatter getInstance() {
        return instance;
    }

    private TextFormatter() {
        smilesPattern = compilePattern();
    }

    public CharSequence parsedText(final Contact contact, final CharSequence text) {
        final int linkColor = Scheme.getColor(Scheme.THEME_LINKS);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        if (contact != null) {
            String userId = contact.getProtocol().getUniqueUserId(contact);
            if (userId.startsWith(JuickMenu.JUICK) || userId.startsWith(JuickMenu.JUBO)) {
                getTextWithLinks(builder, linkColor, JuickMenu.MODE_JUICK);
            } else if (userId.startsWith(JuickMenu.PSTO) || userId.startsWith(JuickMenu.POINT)) {
                getTextWithLinks(builder, linkColor, JuickMenu.MODE_PSTO);
            }
        }
        getTextWithLinks(builder, linkColor, -1);
        detectEmotions(text, builder);
        return builder;
    }

    public CharSequence detectEmotions(CharSequence text, SpannableStringBuilder builder) {
        Matcher matcher = smilesPattern.matcher(text);
        while (matcher.find()) {
            if (!isThereLinks(builder, new int[]{matcher.start(), matcher.end()})) {
                builder.setSpan(new ImageSpan(smiles.getSmileIcon(smiles.buildSmileyToId().get(matcher.group())).getImage()),
                        matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }

    public CharSequence detectEmotions(CharSequence text) {
        Spannable s;
        if (text instanceof Spannable) {
            s = (Spannable) text;
        } else {
            s = Spannable.Factory.getInstance().newSpannable(text);
        }
        Matcher matcher = smilesPattern.matcher(text);
        while (matcher.find()) {
            s.setSpan(new ImageSpan(smiles.getSmileIcon(smiles.buildSmileyToId().get(matcher.group())).getImage()),
                    matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    private Pattern compilePattern() {
        StringBuilder patternString = new StringBuilder(smiles.getSmileTexts().length * 3);
        patternString.append('(');
        for (String s : smiles.getSmileTexts()) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        patternString.replace(patternString.length() - 1, patternString.length(), ")");
        return Pattern.compile(patternString.toString());
    }

    private static boolean isThereLinks(Spannable spannable, int... positions) {
        InternalURLSpan[] spans = spannable.getSpans(0, spannable.length(), InternalURLSpan.class);
        for (int i = 0; i < spans.length; ++i) {
            InternalURLSpan span = spans[i];
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            int lengthPos = positions.length;

            for (int k = 0; k < lengthPos; ++k) {
                int position = positions[k];
                if (spanStart < position && position < spanEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    public void getTextWithLinks(final SpannableStringBuilder ssb, final int linkColor, final int mode) {
        ArrayList<Hyperlink> msgList = new ArrayList<Hyperlink>();
        ArrayList<Hyperlink> linkList = new ArrayList<Hyperlink>();
        switch (mode) {
            case JuickMenu.MODE_JUICK:
                addLinks(msgList, ssb, juickPattern);
                addLinks(msgList, ssb, nickPattern);
                break;
            case JuickMenu.MODE_PSTO:
                addLinks(msgList, ssb, pstoPattern);
                addLinks(msgList, ssb, nickPattern);
                break;
            default:
                addLinks(linkList, ssb, urlPattern);
                break;
        }
        for (Hyperlink link : msgList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb.setSpan(new ForegroundColorSpan(linkColor), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        for (Hyperlink link : linkList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb.setSpan(new ForegroundColorSpan(linkColor), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private final void addLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            Hyperlink spec = new Hyperlink();
            spec.textSpan = s.subSequence(start, end).toString();
            spec.span = new InternalURLSpan(spec.textSpan.toString());
            spec.start = start;
            spec.end = end;
            links.add(spec);
        }
    }

    class Hyperlink {
        String textSpan;
        InternalURLSpan span;
        int start;
        int end;
    }
}
