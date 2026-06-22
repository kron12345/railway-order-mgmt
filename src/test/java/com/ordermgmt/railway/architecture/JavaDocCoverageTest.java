package com.ordermgmt.railway.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Asserts that every Spring service and JPA entity carries a class-level JavaDoc header. ArchUnit
 * cannot help here because comments are stripped at compile time, so we walk {@code src/main/java}
 * directly and look for a {@code /** ... *}{@code /} block immediately preceding the {@code class}
 * / {@code interface} / {@code enum} declaration.
 *
 * <p>Why class-level only: the project explicitly avoids method-level boilerplate (see {@code
 * CLAUDE.md}). The header is the high-leverage spot where a reader learns what a class is for
 * without reading every method.
 */
class JavaDocCoverageTest {

    private static final Path SRC_MAIN_JAVA = Path.of("src/main/java");

    /** Pattern looking for one of the relevant Spring/JPA stereotype annotations. */
    private static final Pattern STEREOTYPE =
            Pattern.compile(
                    "^\\s*@(Service|Entity|Repository|RestController|Component"
                            + "|Configuration|SpringComponent)\\b",
                    Pattern.MULTILINE);

    /**
     * Pattern that matches a class declaration. We capture the kind so the failure message can be
     * specific.
     */
    private static final Pattern CLASS_DECL =
            Pattern.compile(
                    "(?m)^\\s*(?:public\\s+|abstract\\s+|final\\s+|sealed\\s+|non-sealed\\s+)*"
                            + "(class|interface|enum|record)\\s+(\\w+)");

    /**
     * Pattern that matches a JavaDoc block. We search for the closest one <em>preceding</em> a
     * stereotype annotation.
     */
    private static final Pattern JAVADOC_BLOCK =
            Pattern.compile("/\\*\\*[\\s\\S]*?\\*/", Pattern.MULTILINE);

    @Test
    void everyServiceAndEntityHasClassLevelJavaDoc() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(SRC_MAIN_JAVA)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("package-info.java"))
                    .forEach(p -> checkFile(p, violations));
        }
        assertThat(violations)
                .as(
                        "Every @Service / @Entity / @Repository / @RestController / "
                                + "@Component / @Configuration / @SpringComponent class must "
                                + "have a class-level JavaDoc header explaining what it does.")
                .isEmpty();
    }

    private static void checkFile(Path file, List<String> violations) {
        String src;
        try {
            src = Files.readString(file);
        } catch (IOException ex) {
            violations.add(file + ": cannot read (" + ex.getMessage() + ")");
            return;
        }
        Matcher stereo = STEREOTYPE.matcher(src);
        while (stereo.find()) {
            int annotationStart = stereo.start();

            // Find the first class-like declaration after the annotation.
            Matcher decl = CLASS_DECL.matcher(src);
            if (!decl.find(annotationStart)) continue;
            String kind = decl.group(1);
            String name = decl.group(2);

            // Find the closest JavaDoc block preceding the annotation.  We allow
            // any other annotations in between, so we look at the text from the
            // start of file up to the annotation and grab the *last* block.
            String prefix = src.substring(0, annotationStart);
            Matcher doc = JAVADOC_BLOCK.matcher(prefix);
            int lastDocEnd = -1;
            while (doc.find()) {
                lastDocEnd = doc.end();
            }
            if (lastDocEnd < 0) {
                violations.add(file + ": " + kind + " " + name + " has no class-level JavaDoc");
                continue;
            }
            // Reject the case where the JavaDoc is followed by another non-annotation
            // declaration (i.e. it belongs to an earlier class in the file).
            String betweenDocAndAnnotation = src.substring(lastDocEnd, annotationStart);
            if (CLASS_DECL.matcher(betweenDocAndAnnotation).find()) {
                violations.add(
                        file
                                + ": "
                                + kind
                                + " "
                                + name
                                + " has no class-level JavaDoc (found JavaDoc only on an "
                                + "earlier sibling declaration)");
            }
        }
    }
}
