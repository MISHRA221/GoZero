package com.gozero.ingestion.scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Minimal robots.txt evaluator for the "User-agent: *" group: longest matching rule wins, Allow wins ties.
 */
public final class RobotsTxtPolicy {

    private record Rule(boolean allow, String pattern, Pattern regex) {
    }

    private final List<Rule> rules;

    private RobotsTxtPolicy(List<Rule> rules) {
        this.rules = rules;
    }

    public static RobotsTxtPolicy allowAll() {
        return new RobotsTxtPolicy(List.of());
    }

    public static RobotsTxtPolicy parse(String content) {
        List<Rule> rules = new ArrayList<>();
        boolean inWildcardGroup = false;
        boolean previousWasAgent = false;
        for (String raw : content.split("\\R")) {
            String line = raw.replaceFirst("#.*$", "").trim();
            int colon = line.indexOf(':');
            if (line.isEmpty() || colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if (key.equals("user-agent")) {
                if (!previousWasAgent) {
                    inWildcardGroup = false;
                }
                if (value.equals("*")) {
                    inWildcardGroup = true;
                }
                previousWasAgent = true;
            } else {
                previousWasAgent = false;
                if (inWildcardGroup && !value.isEmpty() && (key.equals("allow") || key.equals("disallow"))) {
                    rules.add(new Rule(key.equals("allow"), value, toRegex(value)));
                }
            }
        }
        return new RobotsTxtPolicy(rules);
    }

    public boolean isAllowed(String path) {
        String target = (path == null || path.isEmpty()) ? "/" : path;
        Rule best = null;
        for (Rule rule : rules) {
            if (rule.regex().matcher(target).lookingAt()) {
                boolean longer = best == null || rule.pattern().length() > best.pattern().length();
                boolean tieAllow = best != null && rule.pattern().length() == best.pattern().length() && rule.allow();
                if (longer || tieAllow) {
                    best = rule;
                }
            }
        }
        return best == null || best.allow();
    }

    private static Pattern toRegex(String pattern) {
        StringBuilder sb = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                sb.append(".*");
            } else if (c == '$') {
                sb.append('$');
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(sb.toString());
    }
}
