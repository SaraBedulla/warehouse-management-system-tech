package com.wms.service;

import com.wms.dto.response.AttachmentResponse;
import com.wms.entity.OrderAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    AttachmentResponse upload(Long orderId, MultipartFile file, String uploaderUsername);

    List<AttachmentResponse> listAttachments(Long orderId, String callerUsername, String callerRole);

    OrderAttachment getAttachmentEntity(Long orderId, Long attachmentId, String callerUsername, String callerRole);

    void delete(Long orderId, Long attachmentId, String callerUsername);
}
