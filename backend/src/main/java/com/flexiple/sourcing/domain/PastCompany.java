package com.flexiple.sourcing.domain;

/** One prior role on a candidate's profile. */
public record PastCompany(String company, CompanyType companyType, String title, int years) {}
