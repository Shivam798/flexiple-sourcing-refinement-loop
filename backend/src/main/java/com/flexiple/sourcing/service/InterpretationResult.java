package com.flexiple.sourcing.service;

import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Rubric;

/** What prompt 01 returns: a complete opening brief plus a note for the chat. */
public record InterpretationResult(Filters filters, Rubric rubric, String assistantReply) {}
