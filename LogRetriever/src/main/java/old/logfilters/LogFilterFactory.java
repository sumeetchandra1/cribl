package old.logfilters;

import org.springframework.stereotype.Component;

@Component
public class LogFilterFactory {

    public LogFilter createFilter(String keyword, int limit) {
        LogFilter filter = new BasicFilter();

        if (keyword != null && !keyword.isEmpty()) {
            filter = new KeywordLogFilter(filter, keyword);
        }

        if (limit != -1) {
            filter = new LimitLogFilter(filter, limit);
        }

        return filter;
    }
}
