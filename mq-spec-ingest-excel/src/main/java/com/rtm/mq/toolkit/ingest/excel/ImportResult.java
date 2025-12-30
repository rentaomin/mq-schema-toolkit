package com.rtm.mq.toolkit.ingest.excel;

import com.rtm.mq.toolkit.ir.GenerationReport;
import com.rtm.mq.toolkit.ir.MessageSchema;

/**
 * Container for imported request/response schemas.
 */
public record ImportResult(MessageSchema request, MessageSchema response, GenerationReport report) {
}
