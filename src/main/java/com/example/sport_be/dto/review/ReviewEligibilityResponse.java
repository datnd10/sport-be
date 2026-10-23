package com.example.sport_be.dto.review;
import java.util.UUID;
public record ReviewEligibilityResponse(UUID orderItemId, String orderCode, String attributes) {}
