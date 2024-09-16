package old.logfilters;

import java.util.List;

public class BasicFilter implements LogFilter {
    /**
     * @param logs
     * @return returns logs as is
     */
    @Override
    public List<String> apply(List<String> logs) {
        return logs;
    }
}
