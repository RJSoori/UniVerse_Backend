package com.example.backend_service.marketplace.controller;

import com.example.backend_service.AzureBlobService;
import com.example.backend_service.common.dto.ForgotPasswordRequest;
import com.example.backend_service.common.dto.ResetPasswordRequest;
import com.example.backend_service.common.dto.SendEmailVerificationRequest;
import com.example.backend_service.common.dto.VerifyEmailRequest;
import com.example.backend_service.common.dto.VerifyEmailResponse;
import com.example.backend_service.common.dto.VerifyResetCodeRequest;
import com.example.backend_service.common.dto.VerifyResetCodeResponse;
import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.marketplace.dto.AdminSellerResponse;
import com.example.backend_service.marketplace.dto.ChangePasswordRequest;
import com.example.backend_service.marketplace.dto.ChatMessageResponse;
import com.example.backend_service.marketplace.dto.ConversationResponse;
import com.example.backend_service.marketplace.dto.ListingReportResponse;
import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.ReportItemRequest;
import com.example.backend_service.marketplace.dto.RecordUnitsSoldRequest;
import com.example.backend_service.marketplace.dto.ReverifyRequest;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.dto.SellerReverificationRequestResponse;
import com.example.backend_service.marketplace.dto.SellerUpdateRequest;
import com.example.backend_service.marketplace.dto.SendMessageRequest;
import com.example.backend_service.marketplace.enums.SellerStatus;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import com.example.backend_service.marketplace.service.ChatService;
import com.example.backend_service.marketplace.service.MarketplaceService;
import com.example.backend_service.marketplace.service.SellerEmailVerificationService;
import com.example.backend_service.marketplace.service.SellerJwtService;
import com.example.backend_service.marketplace.service.SellerPasswordResetService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    @Autowired
    private MarketplaceService marketplaceService;

    @Autowired
    private SellerJwtService sellerJwtService;

    @Autowired
    private MarketplaceItemRepository itemRepository;

    @Autowired
    private AzureBlobService azureBlobService;

    @Autowired
    private SellerPasswordResetService sellerPasswordResetService;

    @Autowired
    private SellerEmailVerificationService sellerEmailVerificationService;

    @Autowired
    private ChatService chatService;

    @PostMapping("/sellers/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        sellerPasswordResetService.requestReset(request.email());
        return Map.of("message", "If that email is registered, we've sent a verification code to it.");
    }

    @PostMapping("/sellers/verify-reset-code")
    public VerifyResetCodeResponse verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        String resetToken = sellerPasswordResetService.verifyCode(request.email(), request.code());
        return new VerifyResetCodeResponse(resetToken);
    }

    @PostMapping("/sellers/reset-password")
    public Map<String, String> resetPassword(@RequestBody ResetPasswordRequest request) {
        sellerPasswordResetService.resetPassword(request.email(), request.resetToken(), request.newPassword());
        return Map.of("message", "Password updated successfully. You can now log in.");
    }

    @PostMapping("/sellers/email/send-code")
    public Map<String, String> sendEmailVerificationCode(@RequestBody SendEmailVerificationRequest request) {
        sellerEmailVerificationService.sendCode(request.email());
        return Map.of("message", "Verification code sent.");
    }

    @PostMapping("/sellers/email/verify-code")
    public VerifyEmailResponse verifyEmailCode(@RequestBody VerifyEmailRequest request) {
        String token = sellerEmailVerificationService.verifyCode(request.email(), request.code());
        return new VerifyEmailResponse(token);
    }

    @PostMapping("/sellers/register")
    public ResponseEntity<SellerAuthResponse> registerSeller(
            @RequestParam("storeName") String storeName,
            @RequestParam("email") String email,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("emailVerificationToken") String emailVerificationToken,
            @RequestParam(value = "identityDocument", required = false) MultipartFile identityDocument,
            @RequestParam(value = "shopLogo", required = false) MultipartFile shopLogo,
            @RequestParam(value = "proofOfItems", required = false) MultipartFile proofOfItems
    ) throws IOException {
        // Registration is only allowed once the email has been verified via the
        // send-code/verify-code pair above; this consumes (and single-uses) that token.
        sellerEmailVerificationService.consumeVerification(email, emailVerificationToken);

        SellerRequest request = new SellerRequest();
        request.setStoreName(storeName);
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(password);
        request.setPhone(phone);
        request.setDescription(description);
        request.setEmailVerificationToken(emailVerificationToken);
        if (identityDocument != null && !identityDocument.isEmpty()) {
            request.setIdentityDocumentUrl(azureBlobService.uploadFile(identityDocument));
        }
        if (shopLogo != null && !shopLogo.isEmpty()) {
            request.setShopLogoUrl(azureBlobService.uploadFile(shopLogo));
        }
        if (proofOfItems != null && !proofOfItems.isEmpty()) {
            request.setProofOfItemsUrl(azureBlobService.uploadFile(proofOfItems));
        }

        return ResponseEntity.ok(marketplaceService.registerSeller(request));
    }

    @PostMapping("/sellers/login")
    public ResponseEntity<SellerAuthResponse> loginSeller(@RequestBody SellerLoginRequest request) {
        return ResponseEntity.ok(marketplaceService.loginSeller(request));
    }

    @GetMapping("/sellers/me")
    public ResponseEntity<SellerResponse> getMySellerProfile(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken) {
        if (sellerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long sellerId = sellerJwtService.parse(sellerToken);
        return ResponseEntity.ok(marketplaceService.getSellerById(sellerId));
    }

    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminSellerResponse> getAllSellers() {
        return marketplaceService.getAllSellers();
    }

    @GetMapping("/sellers/{id}")
    public ResponseEntity<SellerResponse> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getSellerById(id));
    }

    @PutMapping("/sellers/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerResponse> verifySeller(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(marketplaceService.updateSellerStatus(id, SellerStatus.valueOf(status.toUpperCase())));
    }

    @PutMapping("/sellers/{id}/lift-ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerResponse> liftSellerBan(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.liftBan(id));
    }

    @PutMapping("/sellers/me")
    public ResponseEntity<SellerResponse> updateMySellerProfile(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @RequestBody SellerUpdateRequest request) {
        if (sellerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long sellerId = sellerJwtService.parse(sellerToken);
        return ResponseEntity.ok(marketplaceService.updateSeller(sellerId, request));
    }

    @PutMapping("/sellers/me/password")
    public Map<String, String> changeMyPassword(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        marketplaceService.changePassword(sellerId, request.currentPassword(), request.newPassword());
        return Map.of("message", "Password updated successfully.");
    }

    @PutMapping("/sellers/me/logo")
    public ResponseEntity<SellerResponse> updateMyShopLogo(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @RequestParam("logo") MultipartFile logo) throws IOException {
        Long sellerId = sellerJwtService.parse(sellerToken);
        String logoUrl = azureBlobService.uploadFile(logo);
        return ResponseEntity.ok(marketplaceService.updateShopLogo(sellerId, logoUrl));
    }

    @PostMapping("/sellers/me/reverify")
    @ResponseStatus(HttpStatus.CREATED)
    public SellerReverificationRequestResponse requestReverification(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @Valid @RequestBody ReverifyRequest request) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        return marketplaceService.submitReverificationRequest(sellerId, request);
    }

    @GetMapping("/items")
    public List<MarketplaceItemResponse> getAllItems() {
        return marketplaceService.getAllItems();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<MarketplaceItemResponse> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getItemById(id));
    }

    @GetMapping("/items/seller/{sellerId}")
    public List<MarketplaceItemResponse> getItemsBySeller(@PathVariable Long sellerId) {
        return marketplaceService.getItemsBySeller(sellerId);
    }

    @GetMapping("/sellers/me/items")
    public List<MarketplaceItemResponse> getMyItems(@RequestHeader("X-Seller-Token") String sellerToken) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        return marketplaceService.getMyItems(sellerId);
    }

    @PostMapping("/items/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingReportResponse reportItem(
            @AuthenticationPrincipal Long studentId,
            @PathVariable Long id,
            @Valid @RequestBody ReportItemRequest request) {
        return marketplaceService.reportItem(id, studentId, request.reason());
    }

    // Chat Endpoints
    // Both the "as buyer" and "as seller" endpoints below accept either identity: a
    // student JWT (cookie/Authorization header) or an X-Seller-Token header. Whichever
    // is present determines which side of the conversation the caller is acting as.

    @PostMapping("/items/{id}/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse startConversation(
            @AuthenticationPrincipal Long studentId,
            @PathVariable Long id) {
        return chatService.startConversation(id, studentId);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> getMyConversations(@AuthenticationPrincipal Long studentId) {
        return chatService.getBuyerConversations(studentId);
    }

    @GetMapping("/sellers/me/conversations")
    public List<ConversationResponse> getMySellerConversations(
            @RequestHeader("X-Seller-Token") String sellerToken) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        return chatService.getSellerConversations(sellerId);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatMessageResponse> getMessages(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @AuthenticationPrincipal Long studentId,
            @PathVariable Long id) {
        Long sellerId = sellerToken != null ? sellerJwtService.parse(sellerToken) : null;
        return chatService.getMessages(id, studentId, sellerId);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @AuthenticationPrincipal Long studentId,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        Long sellerId = sellerToken != null ? sellerJwtService.parse(sellerToken) : null;
        return chatService.sendMessage(id, studentId, sellerId, request.content());
    }

    @PostMapping("/conversations/{id}/messages/image")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendImageMessage(
            @RequestHeader(value = "X-Seller-Token", required = false) String sellerToken,
            @AuthenticationPrincipal Long studentId,
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) throws IOException {
        Long sellerId = sellerToken != null ? sellerJwtService.parse(sellerToken) : null;
        String imageUrl = azureBlobService.uploadFile(image);
        return chatService.sendImageMessage(id, studentId, sellerId, imageUrl);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceItemResponse createItem(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @RequestBody MarketplaceItemRequest request) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        request.setSellerId(sellerId);
        return marketplaceService.createItem(request);
    }

    @PostMapping("/items/{id}/images")
    public MarketplaceItemResponse uploadItemImages(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images) throws IOException {
        Long sellerId = sellerJwtService.parse(sellerToken);
        List<String> imageUrls = new java.util.ArrayList<>();
        for (MultipartFile image : images) {
            imageUrls.add(azureBlobService.uploadFile(image));
        }
        return marketplaceService.addItemImages(id, sellerId, imageUrls);
    }

    @PostMapping("/items/{id}/units-sold")
    public MarketplaceItemResponse recordUnitsSold(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @PathVariable Long id,
            @Valid @RequestBody RecordUnitsSoldRequest request) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        return marketplaceService.recordUnitsSold(id, sellerId, request.quantity());
    }

    @PostMapping("/items/{id}/reopen-for-rent")
    public MarketplaceItemResponse reopenForRent(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @PathVariable Long id) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        return marketplaceService.reopenForRent(id, sellerId);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @PathVariable Long id) {
        Long authSellerId = sellerJwtService.parse(sellerToken);
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        if (!authSellerId.equals(item.getSeller().getId())) {
            throw new ForbiddenException();
        }
        marketplaceService.deleteItem(id);
    }

    // Admin Endpoints
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MarketplaceItemResponse> getPendingItems() {
        return marketplaceService.getPendingItems();
    }

    @PutMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public MarketplaceItemResponse approveItem(@PathVariable Long id) {
        return marketplaceService.approveItem(id);
    }

    @PutMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public MarketplaceItemResponse rejectItem(@PathVariable Long id) {
        return marketplaceService.rejectItem(id);
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListingReportResponse> getOpenReports() {
        return marketplaceService.getOpenReports();
    }

    @PutMapping("/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ListingReportResponse resolveReport(@PathVariable Long id) {
        return marketplaceService.resolveReport(id);
    }

    @PutMapping("/admin/reports/{id}/reinstate")
    @PreAuthorize("hasRole('ADMIN')")
    public ListingReportResponse reinstateListing(@PathVariable Long id) {
        return marketplaceService.reinstateListing(id);
    }

    @GetMapping("/admin/reports/history")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListingReportResponse> getReportHistory() {
        return marketplaceService.getReportHistory();
    }

    @GetMapping("/admin/reverifications")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SellerReverificationRequestResponse> getOpenReverificationRequests() {
        return marketplaceService.getOpenReverificationRequests();
    }

    @PutMapping("/admin/reverifications/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public SellerReverificationRequestResponse resolveReverificationRequest(@PathVariable Long id) {
        return marketplaceService.resolveReverificationRequest(id);
    }
}
