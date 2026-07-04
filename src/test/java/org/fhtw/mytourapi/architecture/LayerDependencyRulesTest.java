package org.fhtw.mytourapi.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LayerDependencyRulesTest {

    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java/org/fhtw/mytourapi");
    private static final Pattern INTERNAL_IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?org\\.fhtw\\.mytourapi\\.([A-Za-z0-9_]+)(?:\\.|;).*");
    private static final Set<String> LAYERS = Set.of(
            "client",
            "config",
            "controller",
            "domain",
            "dto",
            "exception",
            "mapper",
            "repository",
            "security",
            "service"
    );
    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = Map.of(
            "client", Set.of("client", "config", "dto", "exception"),
            "config", Set.of("client", "config", "dto", "security"),
            "controller", Set.of("controller", "dto", "service"),
            "domain", Set.of("domain"),
            "dto", Set.of("dto"),
            "exception", Set.of("dto", "exception"),
            "mapper", Set.of("domain", "dto", "mapper"),
            "repository", Set.of("domain", "repository"),
            "security", Set.of("domain", "dto", "exception", "repository", "security"),
            "service", Set.of("client", "config", "domain", "dto", "exception", "mapper", "repository", "service")
    );

    @Test
    void productionCodeRespectsLayerDependencyRules() throws IOException {
        List<String> violations;
        try (Stream<Path> files = Files.walk(MAIN_SOURCE_ROOT)) {
            violations = files
                    .filter((file) -> file.toString().endsWith(".java"))
                    .flatMap(LayerDependencyRulesTest::violationsIn)
                    .sorted()
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static Stream<String> violationsIn(Path file) {
        String sourceLayer = sourceLayer(file);
        if (sourceLayer == null) {
            return Stream.empty();
        }

        try {
            return Files.readAllLines(file).stream()
                    .map(INTERNAL_IMPORT::matcher)
                    .filter(Matcher::matches)
                    .map((matcher) -> matcher.group(1))
                    .filter(LAYERS::contains)
                    .filter((targetLayer) -> !ALLOWED_DEPENDENCIES.getOrDefault(sourceLayer, Set.of()).contains(targetLayer))
                    .map((targetLayer) -> sourceLayer + " must not depend on " + targetLayer
                            + " in " + MAIN_SOURCE_ROOT.relativize(file));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + file, exception);
        }
    }

    private static String sourceLayer(Path file) {
        Path relativePath = MAIN_SOURCE_ROOT.relativize(file);
        if (relativePath.getNameCount() == 0) {
            return null;
        }

        String firstSegment = relativePath.getName(0).toString();
        return LAYERS.contains(firstSegment) ? firstSegment : null;
    }
}
