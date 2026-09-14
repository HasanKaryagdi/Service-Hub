package io.supportops.ai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
@Component
public class TextSimilarityProvider implements SimilarityProvider {
    private Set<String> tokens(String text){return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")).filter(s->s.length()>1).collect(Collectors.toSet());}
    public double score(String a,String b){var left=tokens(a);var right=tokens(b);var union=new HashSet<>(left);union.addAll(right);left.retainAll(right);return union.isEmpty()?0:(double)left.size()/union.size();}
}
