package olsh.backend.api_gateway.service;


import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.grpc.client.ArticleServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ArticleServiceBasicTest {

    private ArticleService articleService;
    private UploadFileConfiguration uploadFileConfig;

    @BeforeEach
    void setUp() {
        System.out.println("Starting setup...");
        // This runs before each test method
        // Create Mocks for ArticleService's dependencies
        ArticleServiceClient articleServiceClient = org.mockito.Mockito.mock(ArticleServiceClient.class);
        UserService userService = org.mockito.Mockito.mock(UserService.class);
        uploadFileConfig = org.mockito.Mockito.mock(UploadFileConfiguration.class);

        // Set up default config
        org.mockito.Mockito.when(uploadFileConfig.getMaxFileSize()).thenReturn(1024L); // 1KB

        // Create REAL articleService
        articleService = new ArticleService(articleServiceClient, uploadFileConfig, userService);

        System.out.println("Starting up test...");
    }

    @AfterEach
    void tearDown() {
        // This runs after each test method
        System.out.println("Cleaning up test...");
        articleService = null;
        uploadFileConfig = null;
    }

    // Test 1: Simple utility (string conversion) method testing
    @Test
    void convertTimestampToIso_ValidTimestamp_ReturnsIsoString() {
        com.google.protobuf.Timestamp timestamp = com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(1640995200L) // 2022-01-01T00:00:00Z
                .setNanos(0)
                .build();

        // When - Act (call the method we're testing)
        String result = TimestampConverter.convertTimestampToIso(timestamp);

        // Then - Assert the result
        // AssertJ style (preferred)
        assertThat(result)
                .isNotNull()
                .isEqualTo("2022-01-01T00:00:00Z");
    }

    @Test
    void convertTimestampToIso_NullTimestamp_ReturnsNull() {
        // Given
        com.google.protobuf.Timestamp timestamp = null;

        // When
        String result = TimestampConverter.convertTimestampToIso(timestamp);

        // Then
        assertThat(result).isNull();
    }

    // Test 2: File validation logic
    @Test
    void validatePdfFile_ValidPdfFile_DoesNotThrowException() {
        // Given
        MockMultipartFile validPdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        // When & Then - Testing that no exception is thrown
        assertDoesNotThrow(() -> {
            articleService.validatePdfFile(validPdfFile);
        });
    }

    @Test
    void validatePdfFile_NullFile_ThrowsIllegalArgumentException() {
        // Given
        MultipartFile nullFile = null;

        // When & Then - AssertJ way (more readable)
        assertThatThrownBy(() -> articleService.validatePdfFile(nullFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PDF file is required");
    }

    @Test
    void validatePdfFile_EmptyFile_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[0] // Empty content
        );

        // When & Then
        assertThatThrownBy(() -> {
            articleService.validatePdfFile(emptyFile);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PDF file is required");
    }

    @Test
    void validatePdfFile_NonPdfFile_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "document.txt", // Wrong extension
                "text/plain",
                "Text content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> {
            articleService.validatePdfFile(txtFile);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only PDF files are allowed");
    }

    @Test
    void validatePdfFile_FileTooLarge_ThrowsIllegalArgumentException() {
        // Given
        MockMultipartFile largePdfFile = new MockMultipartFile(
                "file",
                "large-document.pdf",
                "application/pdf",
                new byte[(int) uploadFileConfig.getMaxFileSize() + 1]
        );

        // When & Then
        assertThatThrownBy(() -> {
            articleService.validatePdfFile(largePdfFile);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("File size exceeds maximum limit of %d bytes",
                        uploadFileConfig.getMaxFileSize()));
    }


    @Test
    void validatePdfFile_ExactlyMaxSize_DoesNotThrowException() {
        // Given - Testing boundary condition

        MockMultipartFile boundarySizeFile = new MockMultipartFile(
                "file",
                "boundary.pdf",
                "application/pdf",
                new byte[(int) uploadFileConfig.getMaxFileSize()]
        );

        // When & Then
        assertDoesNotThrow(() -> {
            articleService.validatePdfFile(boundarySizeFile);
        });
    }

    @Test
    void validatePdfFile_CaseInsensitivePdfExtension_DoesNotThrowException() {
        // Given - Testing case sensitivity
        MockMultipartFile upperCasePdfFile = new MockMultipartFile(
                "file",
                "DOCUMENT.PDF", // Uppercase extension
                "application/pdf",
                "PDF content".getBytes()
        );

        // When & Then
        assertDoesNotThrow(() -> {
            articleService.validatePdfFile(upperCasePdfFile);
        });
    }

    // Test 3: Get Article tests
    @Test
    void validateGetArticle_NullId_ThrowsIllegalArgumentException() {
        // Given
        Long id = null;

        // When & Then
        assertThatThrownBy(() -> {
            articleService.getArticleById(id);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ArticleId should be provided");
    }

    @Test
    void validateGetArticle_ZeroId_ThrowsIllegalArgumentException() {
        // Given
        Long id = 0L;

        // When & Then
        assertThatThrownBy(() -> {
            articleService.getArticleById(id);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ArticleId should be provided");
    }

}

