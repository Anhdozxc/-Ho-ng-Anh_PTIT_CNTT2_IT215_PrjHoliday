package vn.edu.ptit.holidayplanner.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

public class ImageReplacementCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ImageReplacementCoordinator.class);

    private final ImageStorageService imageStorageService;

    public ImageReplacementCoordinator(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    public void trackReplacement(String previousPublicId, String newPublicId) {
        if (Objects.equals(previousPublicId, newPublicId)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safelyDelete(previousPublicId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    safelyDelete(previousPublicId);
                } else {
                    safelyDelete(newPublicId);
                }
            }
        });
    }

    public void cleanupFailedUpload(String publicId) {
        safelyDelete(publicId);
    }

    private void safelyDelete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            imageStorageService.delete(publicId);
        } catch (ImageStorageException exception) {
            log.warn("Could not clean up managed image {} ({})", publicId,
                    exception.getClass().getSimpleName());
        }
    }
}
