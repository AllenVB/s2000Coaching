package com.s2000coaching.web.dto;

import com.s2000coaching.service.AttentionScorer.Item;
import java.util.List;

/**
 * @param items            members worth a look, most urgent first, each with the reasons
 * @param pendingTransfers orders waiting for the coach to confirm the money arrived
 */
public record AttentionResponse(List<Item> items, List<OrderSummaryResponse> pendingTransfers) {
}
