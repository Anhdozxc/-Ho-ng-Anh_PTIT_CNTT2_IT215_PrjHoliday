package vn.edu.ptit.holidayplanner.media;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ImageReplacementCoordinatorTest {
    @Mock private ImageStorageService imageStorageService;
    private ImageReplacementCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new ImageReplacementCoordinator(imageStorageService);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void commitDeletesOldImageOnlyAfterDatabaseTransactionCompletes() {
        coordinator.trackReplacement("holiday-planner/avatars/old", "holiday-planner/avatars/new");
        verifyNoInteractions(imageStorageService);

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(imageStorageService).delete("holiday-planner/avatars/old");
        verify(imageStorageService, never()).delete("holiday-planner/avatars/new");
    }

    @Test
    void rollbackDeletesNewImageAndPreservesOldImage() {
        coordinator.trackReplacement("holiday-planner/avatars/old", "holiday-planner/avatars/new");

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(imageStorageService).delete("holiday-planner/avatars/new");
        verify(imageStorageService, never()).delete("holiday-planner/avatars/old");
    }

    private void complete(int status) {
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(status);
        }
    }
}
