package com.rtm.mq.spec.excel;

import com.rtm.mq.ir.GenerationReport;
import com.rtm.mq.ir.MessageSchema;

/**
 * Container for imported request/response schemas.
 */
public record ImportResult(MessageSchema request, MessageSchema response, GenerationReport report) {
}
