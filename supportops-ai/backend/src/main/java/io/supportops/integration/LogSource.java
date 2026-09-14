package io.supportops.integration;
import java.util.*;
/** Adapter boundary for an Elasticsearch implementation; preserve deterministic time ordering. */
public interface LogSource { List<Map<String,Object>> forTransactions(List<String> transactionIds); }
