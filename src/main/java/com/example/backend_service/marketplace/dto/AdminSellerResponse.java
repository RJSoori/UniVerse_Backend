package com.example.backend_service.marketplace.dto;

/**
 * Extends SellerResponse with verification document URLs, for admin review only.
 * Kept separate from SellerResponse (which is returned by public endpoints, including
 * embedded in item listings) so document URLs are never exposed to buyers.
 */
public class AdminSellerResponse extends SellerResponse {

    private String identityDocumentUrl;
    private String proofOfItemsUrl;
    private int warningCount;

    public String getIdentityDocumentUrl() { return identityDocumentUrl; }
    public void setIdentityDocumentUrl(String identityDocumentUrl) { this.identityDocumentUrl = identityDocumentUrl; }
    public String getProofOfItemsUrl() { return proofOfItemsUrl; }
    public void setProofOfItemsUrl(String proofOfItemsUrl) { this.proofOfItemsUrl = proofOfItemsUrl; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
}
