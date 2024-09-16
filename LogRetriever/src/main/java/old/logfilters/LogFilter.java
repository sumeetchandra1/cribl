package old.logfilters;

import java.util.List;

public interface LogFilter {

    List<String> apply(List<String> logs);
}
