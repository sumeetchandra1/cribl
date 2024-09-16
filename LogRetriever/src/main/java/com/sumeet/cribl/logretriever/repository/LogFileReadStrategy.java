package com.sumeet.cribl.logretriever.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface LogFileReadStrategy {

    InputStream readFile(Path filePath, String keywords, Integer limit) throws IOException;
}
