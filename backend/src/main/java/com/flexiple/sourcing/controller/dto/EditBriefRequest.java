package com.flexiple.sourcing.controller.dto;

import com.flexiple.sourcing.domain.Filters;
import com.flexiple.sourcing.domain.Rubric;

/** A hand edit from the brief panel. Either half may be null, meaning "leave as is". */
public record EditBriefRequest(Filters filters, Rubric rubric) {}
