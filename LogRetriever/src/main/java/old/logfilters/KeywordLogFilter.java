package old.logfilters;

import java.util.List;
import java.util.stream.Collectors;

public class KeywordLogFilter implements LogFilter {

    private final LogFilter decoratedLogFilter;
    private final String keyword;

    public KeywordLogFilter(LogFilter decoratedLogFilter, String keyword) {
        this.decoratedLogFilter = decoratedLogFilter;
        this.keyword = keyword;
    }

    /**
     * @param logs
     * @return
     */
    @Override
    public List<String> apply(List<String> logs) {

        List<String> filteredLogs = decoratedLogFilter.apply(logs);
        if (keyword == null || keyword.isEmpty()) {
            return logs; // No keyword filtering, return logs as-is
        }

        return filteredLogs.stream()
                .filter(line -> line.contains(keyword))
                .collect(Collectors.toList());
    }
}
