package com.wms.controller;

import com.wms.dto.response.AttachmentResponse;
import com.wms.entity.OrderAttachment;
import com.wms.service.AttachmentService;
import com.wms.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File attachments for orders. Stored in MinIO. CLIENT can upload/delete on editable orders. Both CLIENT and WAREHOUSE_MANAGER can list and download.")
public class AttachmentController {

    private static final Logger log = LogManager.getLogger(AttachmentController.class);

    private final AttachmentService attachmentService;
    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENT', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Upload a file attachment to an order",
               description = "CLIENT: only allowed when order is CREATED or DECLINED. WAREHOUSE_MANAGER: always allowed.")
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable Long orderId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        AttachmentResponse response = attachmentService.upload(orderId, file, userDetails.getUsername());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "List all attachments for an order")
    public ResponseEntity<List<AttachmentResponse>> list(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String role = resolveRole(userDetails);
        List<AttachmentResponse> attachments = attachmentService.listAttachments(
                orderId, userDetails.getUsername(), role);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Download an attachment — streams directly from MinIO")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long orderId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String role = resolveRole(userDetails);
        OrderAttachment attachment = attachmentService.getAttachmentEntity(
                orderId, attachmentId, userDetails.getUsername(), role);

        log.info("User '{}' downloading attachment '{}' from MinIO",
                userDetails.getUsername(), attachment.getOriginalName());

        InputStream stream = storageService.download(attachment.getStoredName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .contentLength(attachment.getFileSize())
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/{attachmentId}/url")
    @PreAuthorize("hasAnyRole('CLIENT', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Get a pre-signed download URL (valid for 1 hour)",
               description = "Returns a temporary direct URL to the file in MinIO. Useful for frontend direct downloads.")
    public ResponseEntity<String> presignedUrl(
            @PathVariable Long orderId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String role = resolveRole(userDetails);
        OrderAttachment attachment = attachmentService.getAttachmentEntity(
                orderId, attachmentId, userDetails.getUsername(), role);

        String url = storageService.generatePresignedUrl(attachment.getStoredName());
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Delete an attachment (CLIENT only, order must be CREATED or DECLINED)")
    public ResponseEntity<Void> delete(
            @PathVariable Long orderId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        attachmentService.delete(orderId, attachmentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    private String resolveRole(UserDetails userDetails) {
        return userDetails.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
    }
}
