package org.wilkretawesomesauce.alchemancystuck.client;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.util.FormattedCharSequence;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Java port of the "sbahjifier" browser extension's {@code modifyText} character-corruption cipher
 * (see the referenced {@code sbahjifier-master} project's {@code injected.js}) - same per-character
 * vowel/consonant substitution chances, dot-run randomization, and their/there homophone confusion,
 * minus the image/CSS-vandalism half of that extension, which has no Minecraft equivalent.
 *
 * <p>Every roll is drawn from a {@link Random} seeded from the text's own content, not a shared/global
 * source - {@code Font} draws the same on-screen string fresh every frame, and reseeding per call keeps
 * a given piece of text corrupted the *same* way every frame instead of flickering into a new random
 * scramble 20+ times a second.
 *
 * <p>{@link #transform(FormattedCharSequence)} only does the fixed-length per-character substitutions
 * (skips the dot-run/homophone passes, which change length) - unlike a plain {@link String}, a
 * {@code FormattedCharSequence} carries a {@link net.minecraft.network.chat.Style} (color, hover/click
 * events) per character, and remapping codepoints in place via
 * {@link FormattedCharSequence#decorateOutput} preserves that; rebuilding the sequence from a
 * different-length flattened string would require deciding which character's style survives at each
 * new position, which isn't worth it for what's ultimately a cosmetic joke.
 */
public final class SbahjTextTransformer {

    private SbahjTextTransformer() {
    }

    private static final String EI = "ei";
    private static final String UO = "uo";
    private static final String VOWELS = "aeiou";
    private static final String VOWEL_TYPO = "aeiouooooaeuiy";
    private static final String CONSONANTS = "bcdfghjklmnpqrstvwxz";
    private static final String CONSONANT_TYPO = "wrtpsddffghjklzxcvbnm";

    private static final Pattern DOT_RUN = Pattern.compile("\\.+");
    private static final Pattern HOMOPHONE = Pattern.compile("their|they're|there|thare|thair|tha're|the're|thei're|thier", Pattern.CASE_INSENSITIVE);
    private static final String[] HOMOPHONES = {"their", "they're", "there", "thare", "thair", "tha're", "the're", "thei're", "thier"};

    /** Full cipher, including the dot-run/homophone passes - for callers that already hold a plain String. */
    public static String transform(String text) {
        if (text.isEmpty())
            return text;

        Random random = new Random(text.hashCode());

        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++)
            builder.append((char) transformChar(text.charAt(i), random));
        String result = builder.toString();

        result = replaceAll(DOT_RUN, result, random, match -> {
            String[] options = {match, match, match, match, match, match, match, match, match, match,
                    match, match, match, match, match, match, match, match, match,
                    " ", "....", ".......", ".....", "..........", "......"};
            return choose(options, random);
        });

        result = replaceAll(HOMOPHONE, result, random, match -> choose(HOMOPHONES, random));

        return result.replace("'ll", "'m");
    }

    /** Fixed-length subset only, so a {@link net.minecraft.network.chat.Style} per character survives. */
    public static FormattedCharSequence transform(FormattedCharSequence sequence) {
        int[] hash = {0};
        sequence.accept((index, style, codePoint) -> {
            hash[0] = hash[0] * 31 + codePoint;
            return true;
        });

        Random random = new Random(hash[0]);
        Int2IntFunction remap = codePoint -> transformChar((char) codePoint, random);

        return sink -> sequence.accept(FormattedCharSequence.decorateOutput(sink, remap));
    }

    private static int transformChar(char c, Random random) {
        c = Character.toLowerCase(c);

        if ((c == 'e' || c == 'i') && random.nextFloat() < 0.4F)
            c = EI.charAt(random.nextInt(EI.length()));
        if ((c == 'u' || c == 'o') && random.nextFloat() < 0.4F)
            c = UO.charAt(random.nextInt(UO.length()));
        if (VOWELS.indexOf(c) >= 0 && random.nextFloat() < 0.001F)
            c = VOWEL_TYPO.charAt(random.nextInt(VOWEL_TYPO.length()));
        if (CONSONANTS.indexOf(c) >= 0 && random.nextFloat() < 0.001F)
            c = CONSONANT_TYPO.charAt(random.nextInt(CONSONANT_TYPO.length()));
        if ((c == 'd' || c == 'g') && random.nextFloat() < 0.3F)
            c = c == 'd' ? 'g' : 'd';

        return c;
    }

    private interface MatchReplacer {
        String replace(String match);
    }

    private static String replaceAll(Pattern pattern, String text, Random random, MatchReplacer replacer) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find())
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacer.replace(matcher.group())));
        matcher.appendTail(result);
        return result.toString();
    }

    private static String choose(String[] options, Random random) {
        return options[random.nextInt(options.length)];
    }
}
