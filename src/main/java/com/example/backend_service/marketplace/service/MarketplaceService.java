package com.example.backend_service.marketplace.service;

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.ConflictException;
import com.example.backend_service.common.exception.ForbiddenException;
import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.common.exception.UnauthorizedException;
import com.example.backend_service.marketplace.dto.AdminSellerResponse;
import com.example.backend_service.marketplace.dto.ListingReportResponse;
import com.example.backend_service.marketplace.dto.MarketplaceItemRequest;
import com.example.backend_service.marketplace.dto.MarketplaceItemResponse;
import com.example.backend_service.marketplace.dto.SellerAuthResponse;
import com.example.backend_service.marketplace.dto.SellerLoginRequest;
import com.example.backend_service.marketplace.dto.SellerRequest;
import com.example.backend_service.marketplace.dto.SellerResponse;
import com.example.backend_service.marketplace.dto.SellerUpdateRequest;
import com.example.backend_service.marketplace.enums.ItemStatus;
import com.example.backend_service.marketplace.enums.ReportStatus;
import com.example.backend_service.marketplace.enums.SellerStatus;
import com.example.backend_service.marketplace.dto.ReverifyRequest;
import com.example.backend_service.marketplace.dto.SellerReverificationRequestResponse;
import com.example.backend_service.marketplace.model.Conversation;
import com.example.backend_service.marketplace.model.ListingReport;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.model.Seller;
import com.example.backend_service.marketplace.model.SellerReverificationRequest;
import com.example.backend_service.marketplace.repository.ConversationRepository;
import com.example.backend_service.marketplace.repository.ListingReportRepository;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import com.example.backend_service.marketplace.repository.SellerRepository;
import com.example.backend_service.marketplace.repository.SellerReverificationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    @Autowired
    private MarketplaceItemRepository itemRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ListingReportRepository listingReportRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private SellerJwtService sellerJwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SellerReverificationRequestRepository reverificationRequestRepository;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("marketplaceEmailService")
    private EmailService emailService;

    // ── Moderation thresholds ──
    // A "strike" is issued each time an admin confirms a takedown (resolveReport). At
    // STRIKE_LIMIT_THRESHOLD strikes, posting is throttled; at STRIKE_BAN_THRESHOLD, the
    // seller is banned outright. Reinstating a previously-taken-down listing (because the
    // report turns out to be bogus) reverses the strike it caused, but never auto-lifts a
    // ban — that always requires an explicit admin review.
    private static final int STRIKE_LIMIT_THRESHOLD = 3;
    private static final int STRIKE_BAN_THRESHOLD = 5;
    private static final int MAX_ACTIVE_LISTINGS_WHEN_RESTRICTED = 3;

    /**
     * Creates a new seller account after validating that email and username are not already in use.
     * Encodes password using BCrypt for secure storage and issues a JWT token for immediate login.
     * Throws RuntimeException if email or username is already registered.
     */
    public SellerAuthResponse registerSeller(SellerRequest request) {
        if (sellerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        if (sellerRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username already taken");
        }
        Seller seller = new Seller();
        seller.setStoreName(request.getStoreName());
        seller.setEmail(request.getEmail());
        seller.setUsername(request.getUsername());
        seller.setPassword(passwordEncoder.encode(request.getPassword()));
        seller.setPhone(request.getPhone());
        seller.setDescription(request.getDescription());
        seller.setIdentityDocumentUrl(request.getIdentityDocumentUrl());
        seller.setShopLogoUrl(request.getShopLogoUrl());
        seller.setProofOfItemsUrl(request.getProofOfItemsUrl());
        Seller saved = sellerRepository.save(seller);
        String token = sellerJwtService.issue(saved.getId());
        return new SellerAuthResponse(token, toSellerResponse(saved));
    }

    /**
     * Authenticates a seller by finding them by username and verifying the password.
     * Issues a JWT token upon successful authentication for use in subsequent requests.
     * Throws RuntimeException if username doesn't exist or password is incorrect.
     */
    public SellerAuthResponse loginSeller(SellerLoginRequest request) {
        Seller seller = sellerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), seller.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        // Verification status no longer gates login — a still-pending (or rejected)
        // seller can log in and use their dashboard the same as at registration; the UI
        // just labels their account's real status instead of claiming they're verified.
        String token = sellerJwtService.issue(seller.getId());
        return new SellerAuthResponse(token, toSellerResponse(seller));
    }

    /**
     * Extracts seller ID from a JWT token and retrieves the corresponding seller profile.
     * Used internally for token-based seller lookups.
     */
    public SellerResponse getSellerByToken(String token) {
        Long sellerId = sellerJwtService.parse(token);
        return getSellerById(sellerId);
    }

    /**
     * Retrieves all sellers registered in the system, including verification document
     * URLs, for admin review. Not for public/buyer-facing use.
     */
    public List<AdminSellerResponse> getAllSellers() {
        return sellerRepository.findAllByOrderByRegisteredAtDesc()
                .stream()
                .map(this::toAdminSellerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific seller by ID. Throws NotFoundException if seller doesn't exist.
     */
    public SellerResponse getSellerById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + id));
        return toSellerResponse(seller);
    }

    /**
 * Updates an existing seller's profile information.
 * Only updates fields that are provided (not null).
 * Throws NotFoundException if seller doesn't exist.
 */
public SellerResponse updateSeller(Long sellerId, SellerUpdateRequest request) {
    Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
    if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
        seller.setStoreName(request.getStoreName());
    }
    if (request.getPhone() != null && !request.getPhone().isBlank()) {
        seller.setPhone(request.getPhone());
    }
    if (request.getDescription() != null) {
        seller.setDescription(request.getDescription());
    }
    if (request.getNotifyNewMessage() != null) {
        seller.setNotifyNewMessage(request.getNotifyNewMessage());
    }
    if (request.getNotifyNewOffer() != null) {
        seller.setNotifyNewOffer(request.getNotifyNewOffer());
    }
    if (request.getNotifyListingExpiry() != null) {
        seller.setNotifyListingExpiry(request.getNotifyListingExpiry());
    }
    if (request.getNotifyPlatformUpdates() != null) {
        seller.setNotifyPlatformUpdates(request.getNotifyPlatformUpdates());
    }
    Seller updated = sellerRepository.save(seller);
    return toSellerResponse(updated);
}

    /**
     * Changes a seller's password after verifying their current password.
     * Throws UnauthorizedException if the current password is incorrect.
     */
    public void changePassword(Long sellerId, String currentPassword, String newPassword) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
        if (!passwordEncoder.matches(currentPassword, seller.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        seller.setPassword(passwordEncoder.encode(newPassword));
        sellerRepository.save(seller);
    }

    /**
     * Updates a seller's shop logo (Azure Blob URL, uploaded by the controller before
     * calling this method).
     */
    public SellerResponse updateShopLogo(Long sellerId, String logoUrl) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
        seller.setShopLogoUrl(logoUrl);
        return toSellerResponse(sellerRepository.save(seller));
    }

    /**
     * Records a seller's request to be re-verified (e.g. after changing store details
     * or identity). Reviewed by an admin in the moderation queue.
     */
    public SellerReverificationRequestResponse submitReverificationRequest(Long sellerId, ReverifyRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
        SellerReverificationRequest reverification = new SellerReverificationRequest();
        reverification.setSeller(seller);
        reverification.setReason(request.reason());
        return toReverificationResponse(reverificationRequestRepository.save(reverification));
    }

    /**
     * Retrieves all open (unresolved) seller re-verification requests. Used by the
     * admin moderation queue.
     */
    public List<SellerReverificationRequestResponse> getOpenReverificationRequests() {
        return reverificationRequestRepository.findByStatusOrderByRequestedAtDesc(ReportStatus.OPEN)
                .stream()
                .map(this::toReverificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Marks a seller re-verification request as resolved. Throws NotFoundException if
     * the request doesn't exist.
     */
    public SellerReverificationRequestResponse resolveReverificationRequest(Long id) {
        SellerReverificationRequest reverification = reverificationRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reverification request not found with id: " + id));
        reverification.setStatus(ReportStatus.RESOLVED);
        return toReverificationResponse(reverificationRequestRepository.save(reverification));
    }

    /**
     * Updates a seller's verification status. Used by admins to approve or reject
     * a seller's marketplace registration.
     * Throws NotFoundException if seller doesn't exist.
     */
    public SellerResponse updateSellerStatus(Long sellerId, SellerStatus status) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
        seller.setStatus(status);
        Seller updated = sellerRepository.save(seller);
        return toSellerResponse(updated);
    }

    /**
     * Creates a new marketplace item after validating that the seller exists.
     * Sets initial status to PENDING_APPROVAL and associates the item with the selling seller.
     * Throws NotFoundException if seller ID doesn't exist in the system.
     */
    public MarketplaceItemResponse createItem(MarketplaceItemRequest request) {
        Seller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + request.getSellerId()));
        if (seller.isBanned()) {
            throw new ForbiddenException("Your account has been suspended due to repeated policy violations. Contact support to appeal.");
        }
        if (seller.getWarningCount() >= STRIKE_LIMIT_THRESHOLD) {
            long activeOrPendingCount = itemRepository.findBySellerId(seller.getId()).stream()
                    .filter(i -> i.getStatus() == ItemStatus.ACTIVE || i.getStatus() == ItemStatus.PENDING_APPROVAL)
                    .count();
            if (activeOrPendingCount >= MAX_ACTIVE_LISTINGS_WHEN_RESTRICTED) {
                throw new ConflictException("Your account is limited to " + MAX_ACTIVE_LISTINGS_WHEN_RESTRICTED
                        + " active listings due to previous policy violations.");
            }
        }
        MarketplaceItem item = new MarketplaceItem();
        item.setItemName(request.getItemName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setType(request.getType());
        item.setCondition(request.getCondition());
        item.setImageUrl(request.getImageUrl());
        item.setCategory(request.getCategory());
        item.setTotalUnits(request.getTotalUnits() != null && request.getTotalUnits() >= 1 ? request.getTotalUnits() : 1);
        item.setSeller(seller);
        // Every listing waits in the admin moderation queue until approved, regardless
        // of the seller's own verification status.
        item.setStatus(ItemStatus.PENDING_APPROVAL);
        MarketplaceItem saved = itemRepository.save(item);
        return toItemResponse(saved);
    }

    // Statuses a buyer should never see, on the main browse feed or a seller's public
    // storefront: still-pending or rejected moderation, taken down, or hidden by a ban.
    private static final List<ItemStatus> STATUSES_HIDDEN_FROM_BUYERS =
            List.of(ItemStatus.PENDING_APPROVAL, ItemStatus.REJECTED, ItemStatus.REMOVED, ItemStatus.SELLER_BANNED);

    /**
     * Retrieves all publicly visible marketplace items (excludes items still awaiting
     * moderation or rejected by an admin). Converts each item to a response DTO including
     * seller information.
     */
    public List<MarketplaceItemResponse> getAllItems() {
        return itemRepository.findByStatusNotInOrderByIdDesc(STATUSES_HIDDEN_FROM_BUYERS)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all items awaiting admin moderation. Used by the admin "Pending Listings" queue.
     */
    public List<MarketplaceItemResponse> getPendingItems() {
        return itemRepository.findByStatusOrderByIdDesc(ItemStatus.PENDING_APPROVAL)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approves a pending item, making it publicly visible.
     * Throws NotFoundException if item doesn't exist.
     */
    public MarketplaceItemResponse approveItem(Long id) {
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        item.setStatus(ItemStatus.ACTIVE);
        return toItemResponse(itemRepository.save(item));
    }

    /**
     * Rejects a pending item, keeping it hidden from public browsing.
     * Throws NotFoundException if item doesn't exist.
     */
    public MarketplaceItemResponse rejectItem(Long id) {
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        item.setStatus(ItemStatus.REJECTED);
        return toItemResponse(itemRepository.save(item));
    }

    /**
     * Retrieves all items listed by a specific seller, filtered by seller ID.
     * Returns empty list if seller has no items.
     */
    public List<MarketplaceItemResponse> getItemsBySeller(Long sellerId) {
        return itemRepository.findBySellerIdOrderByIdDesc(sellerId)
                .stream()
                .filter(i -> !STATUSES_HIDDEN_FROM_BUYERS.contains(i.getStatus()))
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves every item a seller owns, regardless of status — including ones a buyer
     * should never see (pending moderation, rejected, removed, hidden by a ban). Only for
     * the seller's own authenticated inventory view; never expose this publicly.
     */
    public List<MarketplaceItemResponse> getMyItems(Long sellerId) {
        return itemRepository.findBySellerIdOrderByIdDesc(sellerId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves details of a specific marketplace item by ID.
     * Throws NotFoundException if item doesn't exist.
     */
    public MarketplaceItemResponse getItemById(Long id) {
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        item.setViewCount(item.getViewCount() + 1);
        item = itemRepository.save(item);
        return toItemResponse(item);
    }

    /**
     * Records a buyer's report against a listing (e.g. inappropriate content, spam, fraud).
     * Throws NotFoundException if the item or reporting student doesn't exist.
     */
    public ListingReportResponse reportItem(Long itemId, Long studentId, String reason) {
        MarketplaceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found with id: " + studentId));
        // Hide the listing immediately pending admin review, rather than waiting for the
        // report to be resolved.
        item.setStatus(ItemStatus.REMOVED);
        itemRepository.save(item);
        ListingReport report = new ListingReport();
        report.setItem(item);
        report.setReportedBy(student);
        report.setReason(reason);
        return toReportResponse(listingReportRepository.save(report));
    }

    /**
     * Retrieves all open (unresolved) listing reports. Used by the admin moderation queue.
     */
    public List<ListingReportResponse> getOpenReports() {
        return listingReportRepository.findByStatusOrderByReportedAtDesc(ReportStatus.OPEN)
                .stream()
                .map(this::toReportResponse)
                .collect(Collectors.toList());
    }

    /**
     * Resolves a report by taking down the reported listing (removes it from public
     * browsing) and marking it and every other currently-open report against the same
     * listing as resolved — multiple buyers flagging the same listing is one incident,
     * not one strike per complainer. Issues exactly one strike to the seller.
     * Throws NotFoundException if the report doesn't exist.
     */
    @Transactional
    public ListingReportResponse resolveReport(Long reportId) {
        ListingReport report = listingReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + reportId));
        MarketplaceItem item = report.getItem();
        item.setStatus(ItemStatus.REMOVED);
        itemRepository.save(item);
        List<ListingReport> openReportsForItem = listingReportRepository.findByItem(item).stream()
                .filter(r -> r.getStatus() == ReportStatus.OPEN)
                .collect(Collectors.toList());
        LocalDateTime resolvedAt = LocalDateTime.now();
        openReportsForItem.forEach(r -> {
            r.setStatus(ReportStatus.RESOLVED);
            r.setResolvedAt(resolvedAt);
        });
        listingReportRepository.saveAll(openReportsForItem);
        issueStrike(item.getSeller(), report, openReportsForItem.size());
        return toReportResponse(report);
    }

    /**
     * Records a confirmed policy violation against the seller who owns a taken-down
     * listing: notifies them by email, and escalates to a posting restriction or an
     * outright ban once the relevant strike threshold is crossed.
     */
    private void issueStrike(Seller seller, ListingReport report, int reportCount) {
        seller.setWarningCount(seller.getWarningCount() + 1);
        boolean justBanned = !seller.isBanned() && seller.getWarningCount() >= STRIKE_BAN_THRESHOLD;
        if (justBanned) {
            seller.setBanned(true);
        }
        Seller saved = sellerRepository.save(seller);
        if (justBanned) {
            hideAllListings(saved);
        }
        sendStrikeEmail(saved, report, reportCount, justBanned);
    }

    private static final List<ItemStatus> VISIBLE_STATUSES_HIDDEN_ON_BAN =
            List.of(ItemStatus.ACTIVE, ItemStatus.SOLD, ItemStatus.RENTED);

    private void hideAllListings(Seller seller) {
        List<MarketplaceItem> visibleItems = itemRepository.findBySellerId(seller.getId()).stream()
                .filter(i -> VISIBLE_STATUSES_HIDDEN_ON_BAN.contains(i.getStatus()))
                .collect(Collectors.toList());
        visibleItems.forEach(i -> {
            i.setPreviousStatus(i.getStatus());
            i.setStatus(ItemStatus.SELLER_BANNED);
        });
        itemRepository.saveAll(visibleItems);
    }

    private void sendStrikeEmail(Seller seller, ListingReport report, int reportCount, boolean justBanned) {
        String subject = "Your listing was removed - UniVerse Marketplace";
        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;\">");
        body.append("<h2 style=\"color: #4f46e5;\">UniVerse Marketplace</h2>");
        body.append("<p>Your listing <strong>").append(report.getItem().getItemName())
                .append("</strong> has been removed for violating our community guidelines")
                .append(reportCount > 1 ? " (reported by " + reportCount + " different buyers)" : "")
                .append(".</p>");
        body.append("<p><strong>Reason:</strong> ").append(report.getReason()).append("</p>");
        body.append("<p>This is strike ").append(seller.getWarningCount()).append(" of ")
                .append(STRIKE_BAN_THRESHOLD).append(" on your account.</p>");
        if (justBanned) {
            body.append("<p style=\"color: #b91c1c; font-weight: bold;\">Your account has been suspended due to repeated policy violations. ")
                    .append("You can still log in, but you cannot create new listings and your existing listings are no longer visible to buyers. ")
                    .append("Contact support if you believe this is a mistake.</p>");
        } else if (seller.getWarningCount() >= STRIKE_LIMIT_THRESHOLD) {
            body.append("<p style=\"color: #b45309; font-weight: bold;\">Because of repeated violations, your account is now limited to ")
                    .append(MAX_ACTIVE_LISTINGS_WHEN_RESTRICTED).append(" active listings at a time.</p>");
        }
        body.append("<p style=\"color: #6b7280; font-size: 12px; margin-top: 32px;\">UniVerse Marketplace &bull; Do not reply to this automated email.</p>");
        body.append("</div>");
        try {
            emailService.sendHtml(seller.getEmail(), subject, body.toString());
        } catch (Exception e) {
            // A failed notification email shouldn't block the takedown itself.
        }
    }

    /**
     * Reinstates a listing that was hidden by a report the admin has determined to be
     * bogus — restores the item to public browsing and marks the report dismissed.
     * Works whether the report is still open (the listing was auto-hidden on report but
     * never formally taken down) or already resolved (a previous takedown is being
     * reversed on appeal). Throws NotFoundException if the report doesn't exist.
     */
    @Transactional
    public ListingReportResponse reinstateListing(Long reportId) {
        ListingReport report = listingReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + reportId));
        MarketplaceItem item = report.getItem();
        Seller seller = item.getSeller();
        // Every report against this listing that's part of the same incident as the one
        // being reinstated moves together — see resolveReport for why they were batched
        // in the first place. For a still-open report that's every other open report on
        // the listing (they're all complaints about its current live state). For a
        // resolved one, it's only the reports that were resolved in that same
        // resolveReport() call (sharing its resolvedAt) — not every report that happens
        // to currently sit at RESOLVED, which could span multiple separate incidents that
        // piled up on this listing over time without ever being reinstated in between.
        ReportStatus batchStatus = report.getStatus();
        LocalDateTime batchResolvedAt = report.getResolvedAt();
        // Only reverse a strike if this batch was previously taken down (RESOLVED) — a
        // still-OPEN report never issued one in the first place. Reversing a strike never
        // auto-lifts a ban; that always requires an explicit admin review via liftBan.
        if (batchStatus == ReportStatus.RESOLVED) {
            seller.setWarningCount(Math.max(0, seller.getWarningCount() - 1));
            sellerRepository.save(seller);
        }
        List<ListingReport> batch = listingReportRepository.findByItem(item).stream()
                .filter(r -> r.getStatus() == batchStatus)
                .filter(r -> batchStatus != ReportStatus.RESOLVED || java.util.Objects.equals(r.getResolvedAt(), batchResolvedAt))
                .collect(Collectors.toList());
        batch.forEach(r -> r.setStatus(ReportStatus.DISMISSED));
        listingReportRepository.saveAll(batch);
        // Only bring the item back if no OTHER resolved incident is still standing
        // against it — reinstating one takedown shouldn't undo a separate, still-valid
        // one on the same listing. If the seller is still banned, the item stays hidden
        // (via the ban, not this report) until an admin lifts the ban.
        boolean otherTakedownStillStands = listingReportRepository.findByItem(item).stream()
                .anyMatch(r -> r.getStatus() == ReportStatus.RESOLVED);
        if (!otherTakedownStillStands) {
            item.setStatus(seller.isBanned() ? ItemStatus.SELLER_BANNED : ItemStatus.ACTIVE);
            itemRepository.save(item);
        }
        return toReportResponse(report);
    }

    /**
     * Lifts a seller's ban after admin review, restoring public visibility to whichever
     * of their listings were hidden by the ban (not ones individually taken down by a
     * report, which stay hidden).
     */
    @Transactional
    public SellerResponse liftBan(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller not found with id: " + sellerId));
        seller.setBanned(false);
        Seller saved = sellerRepository.save(seller);
        List<MarketplaceItem> hiddenItems = itemRepository.findBySellerId(saved.getId()).stream()
                .filter(i -> i.getStatus() == ItemStatus.SELLER_BANNED)
                .collect(Collectors.toList());
        hiddenItems.forEach(i -> {
            i.setStatus(i.getPreviousStatus() != null ? i.getPreviousStatus() : ItemStatus.ACTIVE);
            i.setPreviousStatus(null);
        });
        itemRepository.saveAll(hiddenItems);
        return toSellerResponse(saved);
    }

    /**
     * Retrieves all resolved or dismissed reports — i.e. every report an admin has
     * already acted on — for the admin takedown history view.
     */
    public List<ListingReportResponse> getReportHistory() {
        return listingReportRepository.findByStatusInOrderByReportedAtDesc(List.of(ReportStatus.RESOLVED, ReportStatus.DISMISSED))
                .stream()
                .map(this::toReportResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates a marketplace item's image URL. Validates that the requesting seller owns the item.
     * Throws NotFoundException if item doesn't exist, ForbiddenException if the seller doesn't own it.
     */
    /**
     * Appends photos to a listing's photo set. The 2-8 photo requirement is enforced by
     * the seller-facing upload flow, not here. The first photo ever added becomes the
     * legacy single imageUrl (kept for anywhere that just needs one thumbnail).
     * Validates that the requesting seller owns the item.
     */
    public MarketplaceItemResponse addItemImages(Long itemId, Long sellerId, List<String> newImageUrls) {
        MarketplaceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
        if (item.getSeller() == null || !sellerId.equals(item.getSeller().getId())) {
            throw new com.example.backend_service.common.exception.ForbiddenException();
        }
        item.getImageUrls().addAll(newImageUrls);
        if ((item.getImageUrl() == null || item.getImageUrl().isBlank()) && !item.getImageUrls().isEmpty()) {
            item.setImageUrl(item.getImageUrls().get(0));
        }
        MarketplaceItem saved = itemRepository.save(item);
        return toItemResponse(saved);
    }

    /**
     * Records that some number of this listing's units have sold (or been rented out) —
     * there's no in-app checkout, so the seller reports this themselves. Once every unit
     * is accounted for, the listing's status flips to SOLD or RENTED so it stops
     * appearing as available. Validates that the requesting seller owns the item.
     */
    public MarketplaceItemResponse recordUnitsSold(Long itemId, Long sellerId, int quantity) {
        MarketplaceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
        if (item.getSeller() == null || !sellerId.equals(item.getSeller().getId())) {
            throw new com.example.backend_service.common.exception.ForbiddenException();
        }
        if (item.getStatus() != ItemStatus.ACTIVE) {
            // Blocks routing around a takedown/ban (REMOVED, SELLER_BANNED) or a still-
            // pending listing (PENDING_APPROVAL, REJECTED) by "selling out" into SOLD.
            throw new ConflictException("This listing isn't currently active, so a sale can't be recorded against it.");
        }
        item.setSoldUnits(Math.min(item.getTotalUnits(), item.getSoldUnits() + quantity));
        if (item.getSoldUnits() >= item.getTotalUnits()) {
            boolean isRental = item.getType() == com.example.backend_service.marketplace.enums.ItemType.RENT;
            item.setStatus(isRental ? ItemStatus.RENTED : ItemStatus.SOLD);
            if (isRental) {
                item.setTimesRented(item.getTimesRented() + 1);
            }
        }
        MarketplaceItem saved = itemRepository.save(item);
        return toItemResponse(saved);
    }

    /**
     * Reopens a RENTED listing for rent again — there's no return-date tracking, so the
     * seller marks it available themselves once they actually get it back. Only valid
     * for RENT-type listings that are currently RENTED. Validates that the requesting
     * seller owns the item.
     */
    public MarketplaceItemResponse reopenForRent(Long itemId, Long sellerId) {
        MarketplaceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
        if (item.getSeller() == null || !sellerId.equals(item.getSeller().getId())) {
            throw new com.example.backend_service.common.exception.ForbiddenException();
        }
        if (item.getType() != com.example.backend_service.marketplace.enums.ItemType.RENT) {
            throw new ConflictException("Only rental listings can be reopened for rent.");
        }
        if (item.getStatus() != ItemStatus.RENTED) {
            throw new ConflictException("This listing isn't currently rented out.");
        }
        item.setSoldUnits(0);
        item.setStatus(ItemStatus.ACTIVE);
        MarketplaceItem saved = itemRepository.save(item);
        return toItemResponse(saved);
    }

    /**
     * Permanently removes a marketplace item from the system.
     * Validates that item exists before attempting deletion.
     * Throws NotFoundException if item ID doesn't exist.
     */
    public void deleteItem(Long id) {
        MarketplaceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        // Reports reference the item via a required FK; clear them first so a reported
        // item can still be deleted instead of failing on a constraint violation.
        listingReportRepository.deleteAll(listingReportRepository.findByItem(item));
        // Conversations span the whole buyer-seller relationship now, not just this one
        // item, so deleting the item should only clear its "currently discussing" item
        // reference — the conversation and its message history survive.
        List<Conversation> conversations = conversationRepository.findByItemId(id);
        conversations.forEach(c -> c.setItem(null));
        conversationRepository.saveAll(conversations);
        itemRepository.delete(item);
    }

    /**
     * Converts a Seller entity to a SellerResponse DTO, excluding sensitive information like password.
     * Maps relevant seller information for API responses.
     */
    private SellerResponse toSellerResponse(Seller seller) {
        SellerResponse response = new SellerResponse();
        response.setId(seller.getId());
        response.setStoreName(seller.getStoreName());
        response.setEmail(seller.getEmail());
        response.setPhone(seller.getPhone());
        response.setDescription(seller.getDescription());
        response.setStatus(seller.getStatus() != null ? seller.getStatus().name() : null);
        response.setRegisteredAt(seller.getRegisteredAt());
        response.setShopLogoUrl(seller.getShopLogoUrl());
        response.setNotifyNewMessage(seller.isNotifyNewMessage());
        response.setNotifyNewOffer(seller.isNotifyNewOffer());
        response.setNotifyListingExpiry(seller.isNotifyListingExpiry());
        response.setNotifyPlatformUpdates(seller.isNotifyPlatformUpdates());
        response.setBanned(seller.isBanned());
        return response;
    }

    /**
     * Converts a SellerReverificationRequest entity to a response DTO, including the
     * requesting seller's display details.
     */
    private SellerReverificationRequestResponse toReverificationResponse(SellerReverificationRequest reverification) {
        SellerReverificationRequestResponse response = new SellerReverificationRequestResponse();
        response.setId(reverification.getId());
        response.setSeller(toSellerResponse(reverification.getSeller()));
        response.setReason(reverification.getReason());
        response.setStatus(reverification.getStatus());
        response.setRequestedAt(reverification.getRequestedAt());
        return response;
    }

    /**
     * Converts a Seller entity to an AdminSellerResponse DTO, including verification
     * document URLs. Only used by admin-only endpoints.
     */
    private AdminSellerResponse toAdminSellerResponse(Seller seller) {
        AdminSellerResponse response = new AdminSellerResponse();
        response.setId(seller.getId());
        response.setStoreName(seller.getStoreName());
        response.setEmail(seller.getEmail());
        response.setPhone(seller.getPhone());
        response.setDescription(seller.getDescription());
        response.setStatus(seller.getStatus() != null ? seller.getStatus().name() : null);
        response.setRegisteredAt(seller.getRegisteredAt());
        response.setIdentityDocumentUrl(seller.getIdentityDocumentUrl());
        response.setShopLogoUrl(seller.getShopLogoUrl());
        response.setProofOfItemsUrl(seller.getProofOfItemsUrl());
        response.setWarningCount(seller.getWarningCount());
        response.setBanned(seller.isBanned());
        return response;
    }

    /**
     * Converts a MarketplaceItem entity to a MarketplaceItemResponse DTO.
     * Includes the associated seller information by converting the seller entity to a response DTO.
     */
    private MarketplaceItemResponse toItemResponse(MarketplaceItem item) {
        MarketplaceItemResponse response = new MarketplaceItemResponse();
        response.setId(item.getId());
        response.setItemName(item.getItemName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setType(item.getType());
        response.setCondition(item.getCondition());
        response.setStatus(item.getStatus());
        response.setImageUrl(item.getImageUrl());
        response.setImageUrls(item.getImageUrls());
        response.setCategory(item.getCategory());
        response.setViewCount(item.getViewCount());
        response.setTotalUnits(item.getTotalUnits());
        response.setSoldUnits(item.getSoldUnits());
        response.setTimesRented(item.getTimesRented());
        if (item.getSeller() != null) {
            response.setSeller(toSellerResponse(item.getSeller()));
        }
        return response;
    }

    /**
     * Converts a ListingReport entity to a ListingReportResponse DTO, including the
     * reported item and reporting student's display details.
     */
    private ListingReportResponse toReportResponse(ListingReport report) {
        ListingReportResponse response = new ListingReportResponse();
        response.setId(report.getId());
        response.setItem(toItemResponse(report.getItem()));
        response.setReportedByName(report.getReportedBy().getName());
        response.setReportedByEmail(report.getReportedBy().getEmail());
        response.setReason(report.getReason());
        response.setStatus(report.getStatus());
        response.setReportedAt(report.getReportedAt());
        return response;
    }
}
