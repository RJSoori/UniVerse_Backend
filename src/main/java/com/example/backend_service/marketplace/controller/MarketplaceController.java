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
import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.dto.SellerUpdateRequest;
import com.example.backend_service.marketplace.enums.SellerStatus;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
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
    public ResponseEntity<SellerAuthResponse> registerSeller(@Valid @RequestBody SellerRequest request) {
        // Registration is only allowed once the email has been verified via the
        // send-code/verify-code pair above; this consumes (and single-uses) that token.
        sellerEmailVerificationService.consumeVerification(request.getEmail(), request.getEmailVerificationToken());
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
    public List<SellerResponse> getAllSellers() {
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

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceItemResponse createItem(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @RequestBody MarketplaceItemRequest request) {
        Long sellerId = sellerJwtService.parse(sellerToken);
        request.setSellerId(sellerId);
        return marketplaceService.createItem(request);
    }

    @PostMapping("/items/{id}/image")
    public MarketplaceItemResponse uploadItemImage(
            @RequestHeader("X-Seller-Token") String sellerToken,
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) throws IOException {
        Long sellerId = sellerJwtService.parse(sellerToken);
        String imageUrl = azureBlobService.uploadFile(image);
        return marketplaceService.updateItemImage(id, sellerId, imageUrl);
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
}
