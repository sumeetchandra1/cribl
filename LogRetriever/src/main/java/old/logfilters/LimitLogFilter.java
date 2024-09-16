package old.logfilters;

import java.util.Collections;
import java.util.List;


public class LimitLogFilter implements LogFilter {
    private final LogFilter decoratedFilter;
    private final int limit;
    private int count = 0;

    public LimitLogFilter(LogFilter decoratedFilter, int limit) {
        this.decoratedFilter = decoratedFilter;
        this.limit = limit;
    }

    @Override
    public List<String> apply(List<String> logEntry) {
        if (limit == -1 || count < limit) {
            List<String> filteredEntry = decoratedFilter.apply(logEntry);
            if (!filteredEntry.isEmpty()) {
                count++;
                return filteredEntry;
            }
        }
        return Collections.emptyList();
    }

    public boolean isLimitReached() {
        return limit != -1 && count >= limit;
    }
}

/*public class LimitLogFilter implements LogFilter {

    private LogFilter logFilter;
    private int limit;

    public LimitLogFilter(LogFilter logFilter, int limit) {
        this.logFilter = logFilter;
        this.limit = limit;
    }


    @Override
    public List<String> apply(List<String> logLines) {
        List<String> filteredLogs = logFilter.apply(logLines);
        if (limit == -1) {
            return filteredLogs; // No limit, return all lines
        }
        return filteredLogs.subList(0, Math.min(limit, filteredLogs.size()));
    }
}*/
