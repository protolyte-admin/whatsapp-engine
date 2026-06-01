package com.whatsapp.engine.campaigns;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.campaigns.dto.CampaignResponse;
import com.whatsapp.engine.campaigns.dto.CreateCampaignRequest;
import com.whatsapp.engine.campaigns.service.CampaignService;
import com.whatsapp.engine.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/campaigns")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created", campaignService.createCampaign(request, user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CampaignResponse>>> getCampaigns(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Campaigns fetched", campaignService.getCampaigns(pageable, user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Campaign fetched", campaignService.getCampaign(id, user)));
    }

    @PostMapping("/{id}/launch")
    public ResponseEntity<ApiResponse<CampaignResponse>> launchCampaign(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Campaign launch started", campaignService.launchCampaign(id, user)));
    }
}
